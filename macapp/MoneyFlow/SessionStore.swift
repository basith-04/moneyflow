import Foundation
import Observation
import Security

private struct TokenKeychain {
    private let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: "com.moneyflow.mac.session",
        kSecAttrAccount as String: APIClient.productionURL.host()!
    ]

    func read() throws -> String? {
        var request = query
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(request as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        try check(status)
        guard let data = result as? Data, let token = String(data: data, encoding: .utf8) else {
            throw KeychainError(status: errSecDecode)
        }
        return token
    }

    func save(_ token: String) throws {
        let data = Data(token.utf8)
        let status = SecItemUpdate(query as CFDictionary, [kSecValueData as String: data] as CFDictionary)
        if status == errSecItemNotFound {
            var item = query
            item[kSecValueData as String] = data
            try check(SecItemAdd(item as CFDictionary, nil))
        } else {
            try check(status)
        }
    }

    func remove() throws {
        let status = SecItemDelete(query as CFDictionary)
        if status != errSecItemNotFound { try check(status) }
    }

    private func check(_ status: OSStatus) throws {
        if status != errSecSuccess { throw KeychainError(status: status) }
    }

    struct KeychainError: LocalizedError {
        let status: OSStatus
        var errorDescription: String? { "MoneyFlow couldn’t access your login Keychain (\(status))." }
    }
}

@MainActor @Observable
final class SessionStore {
    private(set) var isRestoring = true
    private(set) var sessionID: UUID?
    private(set) var data: AccountData?
    private(set) var isRefreshing = false
    private(set) var isSaving = false
    private(set) var lastUpdated: Date?
    private(set) var signInNotice: String?
    private(set) var refreshError: String?
    var actionError: String?

    @ObservationIgnored private var token: String?
    private let api = APIClient()
    private let keychain = TokenKeychain()

    var isBusy: Bool { isRefreshing || isSaving }

    func restore() {
        guard isRestoring else { return }
        defer { isRestoring = false }
        do {
            if let token = try keychain.read() { startSession(token) }
        } catch {
            signInNotice = error.localizedDescription
        }
    }

    func signIn(_ request: LoginRequest) async throws {
        let token = try await api.signIn(request)
        try keychain.save(token)
        startSession(token)
    }

    func register(_ request: RegistrationRequest) async throws {
        try await api.register(request)
        signInNotice = "Account created. Sign in to continue."
    }

    func clearSignInNotice() { signInNotice = nil }

    private func startSession(_ token: String) {
        self.token = token
        signInNotice = nil
        sessionID = UUID()
    }

    func signOut() {
        do {
            try keychain.remove()
            endSession()
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func endSession(notice: String? = nil) {
        token = nil
        data = nil
        lastUpdated = nil
        refreshError = nil
        actionError = nil
        isRefreshing = false
        isSaving = false
        sessionID = nil
        signInNotice = notice
    }

    private func expireSession() {
        let notice = APIError.expired.localizedDescription
        do {
            try keychain.remove()
            endSession(notice: notice)
        } catch {
            endSession(notice: "\(notice) \(error.localizedDescription)")
        }
    }

    func refresh() async {
        guard let token, let id = sessionID, !isBusy else { return }
        await load(token: token, for: id)
    }

    private func load(token: String, for id: UUID) async {
        isRefreshing = true
        refreshError = nil
        defer { if sessionID == id { isRefreshing = false } }
        do {
            let data = try await api.account(token: token)
            guard sessionID == id else { return }
            self.data = data
            lastUpdated = .now
        } catch APIError.expired {
            if sessionID == id { expireSession() }
        } catch {
            guard sessionID == id, !Task.isCancelled else { return }
            refreshError = error.localizedDescription
        }
    }

    private func write(_ operation: (String) async throws -> Void) async throws {
        guard let token, let id = sessionID else { throw APIError.expired }
        guard !isBusy else { throw APIError.operationInProgress }
        isSaving = true
        defer { if sessionID == id { isSaving = false } }
        do {
            try await operation(token)
        } catch APIError.expired {
            if sessionID == id { expireSession() }
            throw APIError.expired
        }
        guard sessionID == id else { return }
        await load(token: token, for: id)
    }

    func saveExpense(_ expense: ExpenseWrite, id: Int?) async throws {
        try await write { try await api.saveExpense(expense, id: id, token: $0) }
    }

    func saveIncome(_ income: IncomeWrite, id: Int?) async throws {
        try await write { try await api.saveIncome(income, id: id, token: $0) }
    }

    func deleteEntry(_ id: Entry.ID) async throws {
        try await write { try await api.deleteEntry(id, token: $0) }
    }

    func addCategory(_ name: String, kind: LedgerKind) async throws {
        try await write { try await api.addCategory(name, kind: kind, token: $0) }
    }

    func deleteCategory(_ id: Int, kind: LedgerKind) async throws {
        try await write { try await api.deleteCategory(id, kind: kind, token: $0) }
    }
}
