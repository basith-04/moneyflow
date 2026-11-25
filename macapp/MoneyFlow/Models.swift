import Foundation

struct LedgerDay: Codable, Comparable, Hashable, Sendable {
    let rawValue: String

    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .autoupdatingCurrent
        return calendar
    }

    init(_ date: Date) {
        let parts = Self.calendar.dateComponents([.year, .month, .day], from: date)
        rawValue = String(format: "%04d-%02d-%02d", parts.year!, parts.month!, parts.day!)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let text = try container.decode(String.self)
        guard let date = Self.date(from: text), LedgerDay(date).rawValue == text else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Expected YYYY-MM-DD")
        }
        rawValue = text
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }

    private static func date(from text: String) -> Date? {
        let parts = text.split(separator: "-")
        guard parts.count == 3, let year = Int(parts[0]), let month = Int(parts[1]),
              let day = Int(parts[2]), (1...9999).contains(year) else { return nil }
        return calendar.date(from: DateComponents(year: year, month: month, day: day, hour: 12))
    }

    var date: Date { Self.date(from: rawValue)! }
    var display: String { date.formatted(.dateTime.day().month(.abbreviated).year()) }
    var month: String { String(rawValue.prefix(7)) }

    static func < (lhs: Self, rhs: Self) -> Bool { lhs.rawValue < rhs.rawValue }
}

enum LedgerKind: String, CaseIterable, Identifiable, Sendable {
    case expense, income

    var id: Self { self }
    var title: String { self == .expense ? "Expenses" : "Income" }
    var singular: String { self == .expense ? "Expense" : "Income" }
    var methodLabel: String { self == .expense ? "Payment Method" : "Credited To" }
    var symbol: String { self == .expense ? "arrow.up.right.circle" : "arrow.down.left.circle" }
    var path: String { self == .expense ? "expenses" : "income" }
    var categoryQuery: [URLQueryItem] { self == .income ? [.init(name: "type", value: "income")] : [] }
    var methods: [String] { self == .expense ? ["Credit Card", "upi", "cash"] : ["Bank Account", "Cash"] }

    func displayMethod(_ value: String?) -> String {
        switch value {
        case "upi": "UPI"
        case "cash": "Cash"
        case nil, "": "Not specified"
        case let value?: value
        }
    }
}

struct Category: Decodable, Identifiable, Hashable, Sendable {
    let id: Int
    let name: String
}

struct Entry: Identifiable, Sendable {
    struct ID: Hashable, Sendable {
        let kind: LedgerKind
        let value: Int
    }

    let id: ID
    let title: String?
    let amount: Decimal
    let day: LedgerDay
    let categoryName: String
    let categoryID: Int
    let note: String?
    let method: String?

    var kind: LedgerKind { id.kind }
    var displayTitle: String { title ?? categoryName }
    var displayMethod: String { kind.displayMethod(method) }
    var noteText: String { note ?? "" }

    func matches(_ search: String) -> Bool {
        search.isEmpty || [displayTitle, categoryName, noteText, displayMethod].contains {
            $0.localizedStandardContains(search)
        }
    }

    static func newestFirst(_ lhs: Entry, _ rhs: Entry) -> Bool {
        if lhs.day != rhs.day { return lhs.day > rhs.day }
        if lhs.id.value != rhs.id.value { return lhs.id.value > rhs.id.value }
        return lhs.kind.rawValue < rhs.kind.rawValue
    }
}

struct ExpenseResponse: Decodable, Sendable {
    let expenseID: Int
    let title: String
    let amount: Decimal
    let date: LedgerDay
    let name: String
    let categoryID: Int
    let note: String?
    let paymentMode: String?

    enum CodingKeys: String, CodingKey {
        case expenseID = "expense_id", categoryID = "category_id", paymentMode = "payment_mode"
        case title, amount, date, name, note
    }

    var entry: Entry {
        Entry(id: .init(kind: .expense, value: expenseID), title: title, amount: amount,
              day: date, categoryName: name, categoryID: categoryID, note: note, method: paymentMode)
    }
}

struct IncomeResponse: Decodable, Sendable {
    let incomeID: Int
    let amount: Decimal
    let date: LedgerDay
    let name: String
    let categoryID: Int
    let note: String?
    let creditedTo: String

    enum CodingKeys: String, CodingKey {
        case incomeID = "income_id", categoryID = "category_id", creditedTo = "credited_to"
        case amount, date, name, note
    }

    var entry: Entry {
        Entry(id: .init(kind: .income, value: incomeID), title: nil, amount: amount,
              day: date, categoryName: name, categoryID: categoryID, note: note, method: creditedTo)
    }
}

struct ExpenseWrite: Encodable, Sendable {
    let title: String
    let amount: Decimal
    let date: LedgerDay
    let categoryID: Int
    let paymentMode: String?
    let note: String?

    enum CodingKeys: String, CodingKey {
        case categoryID = "category_id", paymentMode = "payment_mode"
        case title, amount, date, note
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(title, forKey: .title)
        try container.encode(amount, forKey: .amount)
        try container.encode(date, forKey: .date)
        try container.encode(categoryID, forKey: .categoryID)
        try container.encodeIfPresent(paymentMode, forKey: .paymentMode)
        try container.encode(note, forKey: .note)
    }
}

struct IncomeWrite: Encodable, Sendable {
    let amount: Decimal
    let date: LedgerDay
    let categoryID: Int
    let creditedTo: String
    let note: String?

    enum CodingKeys: String, CodingKey {
        case categoryID = "category_id", creditedTo = "credited_to"
        case amount, date, note
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(amount, forKey: .amount)
        try container.encode(date, forKey: .date)
        try container.encode(categoryID, forKey: .categoryID)
        try container.encode(creditedTo, forKey: .creditedTo)
        try container.encode(note, forKey: .note)
    }
}

struct LedgerFilters: Equatable {
    var from: LedgerDay?
    var through: LedgerDay?
    var minimum: Decimal?
    var maximum: Decimal?
    var categoryID: Int?
    var method: String?

    var isActive: Bool { self != Self() }

    func includes(_ entry: Entry) -> Bool {
        if let from, entry.day < from { return false }
        if let through, entry.day > through { return false }
        if let minimum, entry.amount < minimum { return false }
        if let maximum, entry.amount > maximum { return false }
        if let categoryID, entry.categoryID != categoryID { return false }
        if let method, entry.method != method { return false }
        return true
    }
}

enum Money {
    static let format = Decimal.FormatStyle.Currency(code: "INR", locale: Locale(identifier: "en_IN"))
    static let maximum = Decimal(9_999_999_999) + Decimal(99) / 100

    static func display(_ amount: Decimal) -> String { amount.formatted(format) }

    static func parse(_ text: String, locale: Locale = .current) -> Decimal? {
        let input = text.trimmed
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.generatesDecimalNumbers = true
        formatter.isLenient = false
        let fullRange = NSRange(input.startIndex..<input.endIndex, in: input)
        var parsedRange = fullRange
        var result: AnyObject?
        guard !input.isEmpty,
              (try? formatter.getObjectValue(&result, for: input, range: &parsedRange)) != nil,
              parsedRange == fullRange,
              let value = (result as? NSNumber)?.decimalValue,
              !value.isNaN, value >= 0, value <= maximum else { return nil }
        var scaled = value * 100
        var rounded = Decimal()
        NSDecimalRound(&rounded, &scaled, 0, .plain)
        return scaled == rounded ? value : nil
    }

    static func input(_ amount: Decimal) -> String {
        amount.formatted(Decimal.FormatStyle(locale: .current).grouping(.never))
    }
}

extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
    var nilIfBlank: String? { trimmed.isEmpty ? nil : self }
    // PostgreSQL varchar limits count Unicode code points.
    var databaseLength: Int { unicodeScalars.count }
}
