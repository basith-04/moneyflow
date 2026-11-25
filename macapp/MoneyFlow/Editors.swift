import SwiftUI

struct EditorLayout<Fields: View>: View {
    let title: String
    let action: String
    let error: String?
    let isWorking: Bool
    let isDisabled: Bool
    let submit: () -> Void
    @ViewBuilder let fields: Fields
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title).font(.title2.bold()).accessibilityAddTraits(.isHeader)
            Form { fields }
                .formStyle(.columns)
                .disabled(isWorking)
            if let error { InlineError(message: error) }
            Divider()
            HStack {
                if isWorking { ProgressView().controlSize(.small).accessibilityLabel("Saving") }
                Spacer()
                Button("Cancel", role: .cancel) { dismiss() }
                    .keyboardShortcut(.cancelAction)
                    .disabled(isWorking)
                Button(action, action: submit)
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(isWorking || isDisabled)
            }
        }
        .padding(22)
        .frame(width: 450)
        .interactiveDismissDisabled(isWorking)
    }
}

struct EntryEditor: View {
    let store: SessionStore
    let kind: LedgerKind
    let entry: Entry?
    @Environment(\.dismiss) private var dismiss
    @FocusState private var firstFieldFocused: Bool
    @ViewState private var title: String
    @ViewState private var amount: String
    @ViewState private var date: Date
    @ViewState private var category: Int?
    @ViewState private var method: String?
    @ViewState private var note: String
    @ViewState private var saving = false
    @ViewState private var error: String?

    init(store: SessionStore, kind: LedgerKind, entry: Entry?) {
        self.store = store
        self.kind = kind
        self.entry = entry
        _title = State(initialValue: entry?.title ?? "")
        _amount = State(initialValue: entry.map { Money.input($0.amount) } ?? "")
        _date = State(initialValue: entry?.day.date ?? .now)
        _category = State(initialValue: entry?.categoryID)
        _method = State(initialValue: entry?.method)
        _note = State(initialValue: entry?.note ?? "")
    }

    private var categories: [Category] { store.data?.categories(for: kind) ?? [] }
    private var canEditMethod: Bool { kind == .income || entry == nil }

    var body: some View {
        EditorLayout(title: "\(entry == nil ? "New" : "Edit") \(kind.singular)",
                     action: entry == nil ? "Add \(kind.singular)" : "Save Changes", error: error,
                     isWorking: saving, isDisabled: store.isBusy || categories.isEmpty, submit: submit) {
            if kind == .expense {
                TextField("Title", text: $title).focused($firstFieldFocused)
                amountField
            } else {
                amountField.focused($firstFieldFocused)
            }
            DatePicker("Date", selection: $date, displayedComponents: .date)
            Picker("Category", selection: $category) {
                Text("Select a Category").tag(nil as Int?)
                ForEach(categories) { Text($0.name).tag(Optional($0.id)) }
            }
            if categories.isEmpty {
                Text("Add an \(kind.singular.lowercased()) category in Categories first.")
                    .font(.callout).foregroundStyle(.secondary)
            }
            if canEditMethod {
                Picker(kind.methodLabel, selection: $method) {
                    Text("Select").tag(nil as String?)
                    ForEach(kind.methods, id: \.self) { Text(kind.displayMethod($0)).tag(Optional($0)) }
                }
            } else {
                LabeledContent(kind.methodLabel, value: kind.displayMethod(method))
            }
            TextField("Note", text: $note, prompt: Text("Optional"), axis: .vertical)
                .lineLimit(3...6)
        }
        .task { firstFieldFocused = true }
    }

    private var amountField: some View {
        TextField("Amount (INR)", text: $amount, prompt: Text("0.00"))
    }

    private func submit() {
        guard !saving else { return }
        if kind == .expense && (title.trimmed.isEmpty || title.trimmed.databaseLength > 160) {
            error = "Enter a title up to 160 characters."
            return
        }
        guard let amount = Money.parse(amount), entry != nil || amount > 0 else {
            error = entry == nil
                ? "Enter an amount greater than zero with up to two decimal places."
                : "Enter a nonnegative amount with up to two decimal places."
            return
        }
        guard let category, categories.contains(where: { $0.id == category }) else {
            error = "Select a category."
            return
        }
        if canEditMethod && !kind.methods.contains(method ?? "") {
            error = "Select \(kind == .expense ? "a payment method" : "where the income was credited")."
            return
        }
        let day = LedgerDay(date)
        saving = true
        error = nil
        Task {
            defer { saving = false }
            do {
                if kind == .expense {
                    try await store.saveExpense(.init(title: title.trimmed, amount: amount, date: day, categoryID: category,
                                                       paymentMode: entry == nil ? method : nil, note: note.nilIfBlank),
                                                id: entry?.id.value)
                } else if let method {
                    try await store.saveIncome(.init(amount: amount, date: day, categoryID: category,
                                                      creditedTo: method, note: note.nilIfBlank), id: entry?.id.value)
                }
                dismiss()
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct CategoryEditor: View {
    let store: SessionStore
    let kind: LedgerKind
    @ViewState private var name = ""
    @ViewState private var saving = false
    @ViewState private var error: String?
    @FocusState private var nameFocused: Bool
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        EditorLayout(title: "New \(kind.singular) Category", action: "Add Category", error: error,
                     isWorking: saving, isDisabled: store.isBusy, submit: submit) {
            TextField("Name", text: $name).focused($nameFocused)
        }
        .task { nameFocused = true }
    }

    private func submit() {
        let name = name.trimmed
        guard !name.isEmpty, name.databaseLength <= 120 else {
            error = "Enter a category name up to 120 characters."
            return
        }
        guard store.data?.categories(for: kind).contains(where: { $0.name == name }) != true else {
            error = "A category with this name already exists."
            return
        }
        saving = true
        error = nil
        Task {
            defer { saving = false }
            do {
                try await store.addCategory(name, kind: kind)
                dismiss()
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct FilterEditor: View {
    let kind: LedgerKind?
    let categories: [Category]
    let methods: [String]
    let onApply: (LedgerFilters) -> Void
    @Environment(\.dismiss) private var dismiss
    @ViewState private var category: Int?
    @ViewState private var method: String?
    @ViewState private var minimum: String
    @ViewState private var maximum: String
    @ViewState private var hasStart: Bool
    @ViewState private var hasEnd: Bool
    @ViewState private var start: Date
    @ViewState private var end: Date
    @ViewState private var error: String?

    init(kind: LedgerKind? = nil, categories: [Category] = [], methods: [String] = [], filters: LedgerFilters,
         onApply: @escaping (LedgerFilters) -> Void) {
        self.kind = kind
        self.categories = categories
        self.methods = methods
        self.onApply = onApply
        _category = State(initialValue: filters.categoryID)
        _method = State(initialValue: filters.method)
        _minimum = State(initialValue: filters.minimum.map(Money.input) ?? "")
        _maximum = State(initialValue: filters.maximum.map(Money.input) ?? "")
        _hasStart = State(initialValue: filters.from != nil)
        _hasEnd = State(initialValue: filters.through != nil)
        _start = State(initialValue: filters.from?.date ?? .now)
        _end = State(initialValue: filters.through?.date ?? .now)
    }

    var body: some View {
        EditorLayout(title: kind.map { "Filter \($0.title)" } ?? "Date Range", action: "Apply", error: error,
                     isWorking: false, isDisabled: false, submit: apply) {
            Toggle("Start Date", isOn: $hasStart)
            if hasStart { DatePicker("From", selection: $start, displayedComponents: .date) }
            Toggle("End Date", isOn: $hasEnd)
            if hasEnd { DatePicker("Through", selection: $end, displayedComponents: .date) }
            if let kind {
                TextField("Minimum (INR)", text: $minimum, prompt: Text("Any amount"))
                TextField("Maximum (INR)", text: $maximum, prompt: Text("Any amount"))
                Picker("Category", selection: $category) {
                    Text("All Categories").tag(nil as Int?)
                    ForEach(categories) { Text($0.name).tag(Optional($0.id)) }
                }
                Picker(kind.methodLabel, selection: $method) {
                    Text("Any").tag(nil as String?)
                    ForEach(methods, id: \.self) { Text(kind.displayMethod($0)).tag(Optional($0)) }
                }
            }
            Button("Clear Filters") {
                onApply(.init())
                dismiss()
            }
        }
    }

    private func apply() {
        let from = hasStart ? LedgerDay(start) : nil
        let through = hasEnd ? LedgerDay(end) : nil
        let minAmount = Money.parse(minimum)
        let maxAmount = Money.parse(maximum)
        if let from, let through, from > through {
            error = "The start date must be on or before the end date."
        } else if (!minimum.trimmed.isEmpty && minAmount == nil) || (!maximum.trimmed.isEmpty && maxAmount == nil) {
            error = "Enter nonnegative amounts with up to two decimal places."
        } else if let minAmount, let maxAmount, minAmount > maxAmount {
            error = "The minimum must be less than or equal to the maximum."
        } else {
            onApply(.init(from: from, through: through, minimum: minAmount, maximum: maxAmount,
                          categoryID: category, method: method))
            dismiss()
        }
    }
}
