import SwiftUI

struct LedgerView: View {
    let kind: LedgerKind
    let entries: [Entry]
    @Binding var state: LedgerState
    let canEdit: Bool
    let add: () -> Void
    let edit: (Entry) -> Void
    let inspect: (Entry) -> Void
    let delete: (Entry) -> Void

    private var visibleEntries: [Entry] {
        entries.filter(state.includes).sorted {
            for comparator in state.sort {
                let result = comparator.compare($0, $1)
                if result != .orderedSame { return result == .orderedAscending }
            }
            return Entry.newestFirst($0, $1)
        }
    }

    var body: some View {
        let rows = visibleEntries
        let total = Money.display(rows.reduce(Decimal.zero) { $0 + $1.amount })
        VStack(spacing: 0) {
            if rows.isEmpty {
                if state.filters.isActive || !state.search.trimmed.isEmpty {
                    ContentUnavailableView {
                        Label("No Matching Transactions", systemImage: "magnifyingglass")
                    } description: {
                        Text("Try another search or clear the filters.")
                    } actions: {
                        Button("Clear Search and Filters") { state.search = ""; state.filters = .init() }
                    }
                } else {
                    ContentUnavailableView {
                        Label(kind == .expense ? "No Expenses" : "No Income", systemImage: kind.symbol)
                    } actions: {
                        Button("New \(kind.singular)…", action: add).disabled(!canEdit)
                    }
                }
            } else {
                Table(of: Entry.self, selection: $state.selection, sortOrder: $state.sort) {
                    TableColumn("Date", value: \.day) { entry in
                        Text(entry.day.display).lineLimit(1)
                    }
                    .width(min: 70, ideal: 110, max: 150)
                    if kind == .expense {
                        TableColumn("Title", value: \.displayTitle) { entry in
                            Text(entry.displayTitle).lineLimit(1).help(entry.displayTitle)
                        }
                        .width(min: 72, ideal: 180)
                    }
                    TableColumn("Category", value: \.categoryName) { entry in
                        Text(entry.categoryName).lineLimit(1).help(entry.categoryName)
                    }
                    .width(min: 64, ideal: 138)
                    TableColumn(kind.methodLabel, value: \.displayMethod) { entry in
                        Text(entry.displayMethod).lineLimit(1).help(entry.displayMethod)
                    }
                    .width(min: 78, ideal: 112, max: 175)
                    TableColumn("Amount", value: \.amount) { entry in
                        Text(Money.display(entry.amount))
                            .monospacedDigit().lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .help(Money.display(entry.amount))
                    }
                    .width(min: 86, ideal: 124, max: 190)
                } rows: {
                    ForEach(rows) { TableRow($0) }
                }
                .contextMenu(forSelectionType: Entry.ID.self) { ids in
                    if let entry = rows.first(where: { ids.contains($0.id) }), ids.count == 1 {
                        Button("Show in Inspector") { inspect(entry) }
                        Divider()
                        Button("Edit \(kind.singular)…") { edit(entry) }.disabled(!canEdit)
                        Button("Delete \(kind.singular)…", role: .destructive) { delete(entry) }.disabled(!canEdit)
                    }
                } primaryAction: { ids in
                    if let entry = rows.first(where: { ids.contains($0.id) }) { inspect(entry) }
                }
            }
            Divider()
            HStack(spacing: 8) {
                Text(rows.count == 1 ? "1 transaction" : "\(rows.count) transactions")
                    .foregroundStyle(.secondary)
                if state.filters.isActive {
                    Button("Clear Filters") { state.filters = .init() }
                        .buttonStyle(.link)
                }
                Spacer()
                Text("Total").foregroundStyle(.secondary)
                Text(total).fontWeight(.semibold)
            }
            .font(.callout)
            .monospacedDigit()
            .padding(.horizontal, 18)
            .padding(.vertical, 9)
        }
    }
}

struct EntryInspector: View {
    let entry: Entry?
    let edit: () -> Void
    let delete: () -> Void
    let actionsEnabled: Bool

    var body: some View {
        if let entry {
            ScrollView {
                VStack(alignment: .leading, spacing: 15) {
                    Label(entry.kind.singular, systemImage: entry.kind.symbol)
                        .font(.callout).foregroundStyle(.secondary)
                    Text(Money.display(entry.amount))
                        .font(.title2.weight(.semibold)).monospacedDigit().textSelection(.enabled)
                    Text(entry.displayTitle)
                        .font(.headline).textSelection(.enabled)
                        .accessibilityAddTraits(.isHeader)
                    Divider()
                    detail("Date", entry.day.display)
                    if entry.kind == .expense { detail("Category", entry.categoryName) }
                    detail(entry.kind.methodLabel, entry.displayMethod)
                    detail("Note", entry.note?.nilIfBlank ?? "No note")
                    Divider()
                    HStack {
                        Button("Edit…", action: edit)
                        Spacer()
                        Button("Delete…", role: .destructive, action: delete)
                    }
                    .disabled(!actionsEnabled)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else {
            ContentUnavailableView {
                Label("No Selection", systemImage: "sidebar.right")
            } description: {
                Text("Select a transaction to see its details.")
            }
        }
    }

    private func detail(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).textSelection(.enabled)
        }
        .accessibilityElement(children: .combine)
    }
}
