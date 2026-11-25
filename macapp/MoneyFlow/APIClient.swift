import Foundation

struct LoginRequest: Encodable, Sendable {
    let userName: String
    let password: String
}

struct RegistrationRequest: Encodable, Sendable {
    let fullName: String
    let userName: String
    let email: String
    let password: String
}

struct AccountData: Sendable {
    let expenses: [Entry]
    let income: [Entry]
    let expenseCategories: [Category]
    let incomeCategories: [Category]

    func entries(for kind: LedgerKind) -> [Entry] { kind == .expense ? expenses : income }
    func categories(for kind: LedgerKind) -> [Category] { kind == .expense ? expenseCategories : incomeCategories }
}

enum APIError: LocalizedError {
    case expired
    case response(Int, String?)
    case unexpectedResponse
    case operationInProgress

    var errorDescription: String? {
        switch self {
        case .expired: "Your session ended. Sign in again to continue."
        case let .response(status, message): message ?? "MoneyFlow returned an error (\(status))."
        case .unexpectedResponse: "MoneyFlow returned an unexpected response. Try refreshing."
        case .operationInProgress: "Wait for the current operation to finish and try again."
        }
    }
}

struct APIClient: Sendable {
    private struct Token: Decodable { let token: String }
    private struct Acknowledgment: Decodable { let message: String }
    private struct CategoryWrite: Encodable { let name: String }
    private struct Failure: Decodable {
        let error: String?
        let message: String?
    }

    private enum Method: String {
        case get = "GET", post = "POST", patch = "PATCH", delete = "DELETE"
    }

    private final class RedirectPolicy: NSObject, URLSessionTaskDelegate {
        func urlSession(_ session: URLSession, task: URLSessionTask,
                        willPerformHTTPRedirection response: HTTPURLResponse,
                        newRequest request: URLRequest,
                        completionHandler: @escaping @Sendable (URLRequest?) -> Void) {
            completionHandler(nil)
        }
    }

    static let productionURL = URL(string: "https://moneyflow-czo7.onrender.com")!
    private let baseURL: URL
    private let session: URLSession

    init(baseURL: URL = productionURL, session: URLSession? = nil) {
        self.baseURL = baseURL
        let configuration = URLSessionConfiguration.ephemeral
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.httpCookieStorage = nil
        configuration.urlCache = nil
        configuration.timeoutIntervalForRequest = 65
        configuration.timeoutIntervalForResource = 90
        self.session = session ?? URLSession(configuration: configuration, delegate: RedirectPolicy(), delegateQueue: nil)
    }

    func signIn(_ credentials: LoginRequest) async throws -> String {
        let result: Token = try await request(.post, path: ["auth", "login"], body: encoded(credentials))
        guard !result.token.isEmpty else { throw APIError.unexpectedResponse }
        return result.token
    }

    func register(_ credentials: RegistrationRequest) async throws {
        let _: Acknowledgment = try await request(.post, path: ["auth", "register"], body: encoded(credentials))
    }

    func account(token: String) async throws -> AccountData {
        async let expenses: [ExpenseResponse] = request(.get, path: [LedgerKind.expense.path], token: token)
        async let income: [IncomeResponse] = request(.get, path: [LedgerKind.income.path], token: token)
        async let expenseCategories = categories(kind: .expense, token: token)
        async let incomeCategories = categories(kind: .income, token: token)
        return try await AccountData(expenses: expenses.map(\.entry), income: income.map(\.entry),
                                     expenseCategories: expenseCategories, incomeCategories: incomeCategories)
    }

    private func categories(kind: LedgerKind, token: String) async throws -> [Category] {
        try await request(.get, path: ["category"], query: kind.categoryQuery, token: token)
    }

    func saveExpense(_ expense: ExpenseWrite, id: Int?, token: String) async throws {
        try await save(expense, kind: .expense, id: id, token: token)
    }

    func saveIncome(_ income: IncomeWrite, id: Int?, token: String) async throws {
        try await save(income, kind: .income, id: id, token: token)
    }

    private func save(_ entry: some Encodable, kind: LedgerKind, id: Int?, token: String) async throws {
        let path = [kind.path] + (id.map { [String($0)] } ?? [])
        let _: Acknowledgment = try await request(id == nil ? .post : .patch, path: path,
                                                  body: encoded(entry), token: token)
    }

    func deleteEntry(_ id: Entry.ID, token: String) async throws {
        let _: Acknowledgment = try await request(.delete, path: [id.kind.path, String(id.value)], token: token)
    }

    func addCategory(_ name: String, kind: LedgerKind, token: String) async throws {
        let _: Acknowledgment = try await request(.post, path: ["category"], query: kind.categoryQuery,
                                                  body: encoded(CategoryWrite(name: name)), token: token)
    }

    func deleteCategory(_ id: Int, kind: LedgerKind, token: String) async throws {
        let _: Acknowledgment = try await request(.delete, path: ["category", String(id)],
                                                  query: kind.categoryQuery, token: token)
    }

    private func encoded(_ value: some Encodable) throws -> Data { try JSONEncoder().encode(value) }

    private func request<Response: Decodable>(_ method: Method, path: [String], query: [URLQueryItem] = [],
                                               body: Data? = nil, token: String? = nil) async throws -> Response {
        let endpoint = path.reduce(baseURL) { $0.appendingPathComponent($1) }
        guard var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false) else {
            throw APIError.unexpectedResponse
        }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw APIError.unexpectedResponse }
        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)
        guard let response = response as? HTTPURLResponse else { throw APIError.unexpectedResponse }
        if response.statusCode == 401 && token != nil { throw APIError.expired }
        guard (200..<300).contains(response.statusCode) else {
            let failure = try? JSONDecoder().decode(Failure.self, from: data)
            throw APIError.response(response.statusCode, failure?.error ?? failure?.message)
        }
        do {
            return try JSONDecoder().decode(Response.self, from: data)
        } catch is DecodingError {
            throw APIError.unexpectedResponse
        }
    }
}
