import Charts
import SwiftUI

enum DashboardPreset: String, CaseIterable, Identifiable {
    case thisMonth = "This Month", lastMonth = "Last Month", thisYear = "This Year", all = "All Dates"
    var id: Self { self }
}

enum DashboardRange: Equatable {
    case preset(DashboardPreset)
    case custom(LedgerFilters)

    static let thisMonth = Self.preset(.thisMonth)

    func filters(now: Date = .now) -> LedgerFilters {
        let calendar = LedgerDay.calendar
        switch self {
        case .preset(.all): return .init()
        case .custom(let filters): return filters
        case .preset(let preset):
            let reference = preset == .lastMonth ? calendar.date(byAdding: .month, value: -1, to: now)! : now
            let interval = calendar.dateInterval(of: preset == .thisYear ? .year : .month, for: reference)!
            let lastDay = calendar.date(byAdding: .day, value: -1, to: interval.end)!
            return .init(from: LedgerDay(interval.start), through: LedgerDay(lastDay))
        }
    }

    var title: String {
        switch self {
        case .preset(let preset): preset.rawValue
        case .custom(let filters):
            switch (filters.from, filters.through) {
            case let (from?, end?): "\(from.display) – \(end.display)"
            case let (from?, nil): "From \(from.display)"
            case let (nil, end?): "Through \(end.display)"
            case (nil, nil): DashboardPreset.all.rawValue
            }
        }
    }
}

struct DashboardSummary {
    struct Total: Identifiable {
        let name: String
        let amount: Decimal
        var id: String { name }
    }

    struct Month: Identifiable {
        let date: Date
        let amount: Decimal
        var id: Date { date }
    }

    let totalIncome: Decimal
    let totalExpenses: Decimal
    let biggestExpense: Decimal?
    let expenseCount: Int
    let categories: [Total]
    let payments: [Total]
    let months: [Month]
    let recent: [Entry]
    var netCashFlow: Decimal { totalIncome - totalExpenses }
    let hasTrendData: Bool

    init(data: AccountData, range: DashboardRange, now: Date = .now) {
        let filters = range.filters(now: now)
        let expenses = data.expenses.filter(filters.includes)
        let income = data.income.filter(filters.includes)
        totalIncome = income.reduce(.zero) { $0 + $1.amount }
        totalExpenses = expenses.reduce(.zero) { $0 + $1.amount }
        biggestExpense = expenses.map(\.amount).max()
        expenseCount = expenses.count
        categories = Self.totals(expenses, key: \.categoryName)
        payments = Self.totals(expenses, key: \.displayMethod)
        recent = Array((expenses + income).sorted(by: Entry.newestFirst).prefix(6))

        let calendar = LedgerDay.calendar
        let thisMonth = calendar.dateInterval(of: .month, for: now)!.start
        // Month and year presets end in the future. Anchor the history at the current month.
        let end = min(filters.through?.date ?? now, now)
        let lastMonth = calendar.dateInterval(of: .month, for: end)?.start ?? thisMonth
        let dates = (-5...0).compactMap { calendar.date(byAdding: .month, value: $0, to: lastMonth) }
        let byMonth = Dictionary(grouping: data.expenses) { $0.day.month }
        hasTrendData = dates.contains { byMonth[LedgerDay($0).month] != nil }
        months = dates.map { date in
            let items = byMonth[LedgerDay(date).month, default: []]
            return Month(date: date, amount: items.reduce(.zero) { $0 + $1.amount })
        }
    }

    private static func totals(_ entries: [Entry], key: KeyPath<Entry, String>) -> [Total] {
        Dictionary(grouping: entries) { $0[keyPath: key] }
            .map { Total(name: $0.key, amount: $0.value.reduce(.zero) { $0 + $1.amount }) }
            .sorted {
                if $0.amount != $1.amount { return $0.amount > $1.amount }
                return $0.name.localizedStandardCompare($1.name) == .orderedAscending
            }
    }
}

struct DashboardView: View {
    let data: AccountData
    let range: DashboardRange
    let openEntry: (Entry) -> Void
    @ViewState private var selectedMonth: Date?

    var body: some View {
        let summary = DashboardSummary(data: data, range: range)
        ScrollView {
            VStack(alignment: .leading, spacing: 23) {
                overview(summary)
                Divider()
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .top, spacing: 34) {
                        trend(summary).frame(minWidth: 300)
                        comparison(summary).frame(minWidth: 240)
                    }
                    VStack(alignment: .leading, spacing: 22) {
                        trend(summary)
                        Divider()
                        comparison(summary)
                    }
                }
                Divider()
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .top, spacing: 34) {
                        categoryBreakdown(summary).frame(minWidth: 300)
                        paymentBreakdown(summary).frame(minWidth: 240)
                    }
                    VStack(alignment: .leading, spacing: 22) {
                        categoryBreakdown(summary)
                        Divider()
                        paymentBreakdown(summary)
                    }
                }
                Divider()
                recentActivity(summary.recent)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 23)
            .frame(maxWidth: 1200)
            .frame(maxWidth: .infinity)
        }
        .onChange(of: range) { selectedMonth = nil }
    }

    private func overview(_ summary: DashboardSummary) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Label(range.title, systemImage: "calendar")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
                .accessibilityLabel("Date range: \(range.title)")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 18, alignment: .leading), count: 3),
                      alignment: .leading, spacing: 16) {
                metric("Income", symbol: LedgerKind.income.symbol, value: summary.totalIncome)
                metric("Expenses", symbol: LedgerKind.expense.symbol, value: summary.totalExpenses)
                metric("Net Cash Flow", symbol: "plus.forwardslash.minus", value: summary.netCashFlow)
            }
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 14) {
                    largestExpense(summary)
                    Divider().frame(height: 14)
                    expenseCount(summary)
                    Divider().frame(height: 14)
                    topCategory(summary)
                }
                VStack(alignment: .leading, spacing: 5) {
                    largestExpense(summary)
                    expenseCount(summary)
                    topCategory(summary)
                }
            }
            .font(.callout)
        }
    }

    private func metric(_ title: String, symbol: String, value: Decimal) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Label(title, systemImage: symbol).font(.subheadline).foregroundStyle(.secondary)
            Text(Money.display(value))
                .font(.system(.largeTitle, design: .default, weight: .semibold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.62)
        }
        .accessibilityElement(children: .combine)
    }

    private func largestExpense(_ summary: DashboardSummary) -> some View {
        detail("Largest expense", summary.biggestExpense.map(Money.display) ?? "—")
    }

    private func expenseCount(_ summary: DashboardSummary) -> some View {
        detail("Expense transactions", summary.expenseCount.formatted())
    }

    private func topCategory(_ summary: DashboardSummary) -> some View {
        detail("Top category", summary.categories.first?.name ?? "—")
    }

    private func detail(_ title: String, _ value: String) -> some View {
        Text("\(Text(title).foregroundStyle(.secondary)) \(Text(value).fontWeight(.medium))")
            .monospacedDigit()
    }

    private func trend(_ summary: DashboardSummary) -> some View {
        let selected = selectedMonth.flatMap { date in
            summary.months.min { abs($0.date.timeIntervalSince(date)) < abs($1.date.timeIntervalSince(date)) }
        }
        return VStack(alignment: .leading, spacing: 8) {
            heading("Monthly Spending")
            if summary.hasTrendData {
                Text(selected.map { "\($0.date.formatted(.dateTime.month(.wide).year())) · \(Money.display($0.amount))" }
                     ?? "Six calendar months")
                    .font(.callout).foregroundStyle(.secondary).monospacedDigit()
                Chart {
                    ForEach(summary.months) { month in
                        LineMark(x: .value("Month", month.date), y: .value("Spending", chartValue(month.amount)))
                            .foregroundStyle(Color.accentColor)
                            .symbol(.circle)
                            .accessibilityLabel(month.date.formatted(.dateTime.month(.wide).year()))
                            .accessibilityValue(Money.display(month.amount))
                    }
                    if let selected {
                        RuleMark(x: .value("Selected month", selected.date))
                            .foregroundStyle(.secondary.opacity(0.5))
                            .accessibilityHidden(true)
                    }
                }
                .chartXSelection(value: $selectedMonth)
                .chartXScale(range: .plotDimension(padding: 16))
                .chartXAxis {
                    AxisMarks(preset: .aligned, values: summary.months.map(\.date)) { _ in
                        AxisValueLabel(format: .dateTime.month(.abbreviated), anchor: .top, collisionResolution: .disabled)
                    }
                }
                .chartYAxis { moneyAxis }
                .chartYScale(domain: 0...max(1, chartValue(summary.months.map(\.amount).max() ?? 0) * 1.12))
                .frame(height: 150)
                .accessibilityLabel("Spending for the last six calendar months")
            } else {
                empty("No expenses in these six months.")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func comparison(_ summary: DashboardSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            heading("Income and Expenses")
            if summary.totalIncome != 0 || summary.totalExpenses != 0 {
                Text(range.title).font(.callout).foregroundStyle(.secondary)
                Chart {
                    BarMark(x: .value("Income", chartValue(summary.totalIncome)), y: .value("Type", "Income"), height: .fixed(16))
                        .foregroundStyle(Color.teal)
                        .accessibilityValue(Money.display(summary.totalIncome))
                    BarMark(x: .value("Expenses", chartValue(summary.totalExpenses)), y: .value("Type", "Expenses"), height: .fixed(16))
                        .foregroundStyle(Color.accentColor)
                        .accessibilityValue(Money.display(summary.totalExpenses))
                }
                .chartXAxis { moneyAxis }
                .chartYAxis {
                    AxisMarks(preset: .aligned, position: .leading) { _ in
                        AxisValueLabel(centered: true, anchor: .trailing)
                    }
                }
                .chartXScale(domain: 0...max(1, chartValue(max(summary.totalIncome, summary.totalExpenses)) * 1.15))
                .frame(height: 150)
                .accessibilityLabel("Income and expenses for \(range.title)")
            } else {
                empty("No income or expenses with a nonzero amount in this range.")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func categoryBreakdown(_ summary: DashboardSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            heading("Spending by Category")
            if summary.categories.isEmpty {
                empty("No expenses in this range.", height: 70)
            } else {
                let values = Array(summary.categories.prefix(8))
                Chart(values) { row in
                    BarMark(x: .value("Spending", chartValue(row.amount)), y: .value("Category", row.name), height: .fixed(8))
                        .foregroundStyle(Color.accentColor)
                        .accessibilityValue(Money.display(row.amount))
                }
                .chartXAxis { moneyAxis }
                .chartYAxis {
                    AxisMarks(preset: .aligned, position: .leading) { value in
                        if let name = value.as(String.self) {
                            AxisValueLabel(centered: true, anchor: .trailing, collisionResolution: .disabled) {
                                Text(name).lineLimit(1).truncationMode(.tail).frame(maxWidth: 124, alignment: .trailing)
                            }
                        }
                    }
                }
                .chartXScale(domain: 0...max(1, chartValue(values.first?.amount ?? 0) * 1.12))
                .frame(height: CGFloat(max(80, values.count * 24 + 24)))
                .accessibilityLabel("Spending by category")
                if summary.categories.count > values.count {
                    Text("Top \(values.count) of \(summary.categories.count) categories")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func paymentBreakdown(_ summary: DashboardSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            heading("Payment Methods")
            if summary.payments.isEmpty {
                empty("No expenses in this range.", height: 70)
            } else {
                ForEach(summary.payments) { row in
                    HStack {
                        Text(row.name)
                        Spacer()
                        Text(Money.display(row.amount)).monospacedDigit().foregroundStyle(.secondary)
                    }
                    .accessibilityElement(children: .combine)
                    if row.id != summary.payments.last?.id { Divider() }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func recentActivity(_ rows: [Entry]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            heading("Recent Transactions")
            if rows.isEmpty {
                empty("No transactions in this range.", height: 52)
            } else {
                ForEach(rows) { entry in
                    Button {
                        openEntry(entry)
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: entry.kind.symbol).foregroundStyle(.secondary).accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(entry.displayTitle).fontWeight(.medium).lineLimit(1)
                                Text(entry.kind == .expense ? entry.categoryName : entry.displayMethod)
                                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                            }
                            Spacer()
                            Text(entry.day.display).foregroundStyle(.secondary).lineLimit(1)
                            Text(Money.display(entry.kind == .expense ? -entry.amount : entry.amount))
                                .monospacedDigit().lineLimit(1)
                                .frame(minWidth: 85, alignment: .trailing)
                            Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                                .accessibilityHidden(true)
                        }
                        .padding(.vertical, 4)
                        .contentShape(.rect)
                        .accessibilityElement(children: .combine)
                    }
                    .buttonStyle(.plain)
                    .help("Show \(entry.displayTitle) in the inspector")
                    if entry.id != rows.last?.id { Divider() }
                }
            }
        }
    }

    private func heading(_ title: String) -> some View {
        Text(title).font(.headline).accessibilityAddTraits(.isHeader)
    }

    private func empty(_ text: String, height: CGFloat = 169) -> some View {
        Text(text).font(.callout).foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, minHeight: height, alignment: .leading)
    }

    private var moneyAxis: some AxisContent {
        AxisMarks(preset: .aligned, values: .automatic(desiredCount: 4)) { value in
            AxisGridLine()
            AxisValueLabel {
                if let amount = value.as(Double.self) {
                    Text("₹\(amount.formatted(.number.notation(.compactName).precision(.fractionLength(0...1))))")
                        .monospacedDigit()
                }
            }
        }
    }

    private func chartValue(_ amount: Decimal) -> Double { NSDecimalNumber(decimal: amount).doubleValue }
}
