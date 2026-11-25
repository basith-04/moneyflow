import SwiftUI

private enum WorkspaceSection: String, CaseIterable, Identifiable {
    case dashboard = "Dashboard", expenses = "Expenses", income = "Income", categories = "Categories"
    var id: Self { self }
    var ledger: LedgerKind? {
        switch self {
        case .expenses: .expense
        case .income: .income
        default: nil
        }
    }
    var symbol: String {
        switch self {
        case .dashboard: "chart.bar.xaxis"
        case .categories: "tag"
        default: ledger?.symbol ?? ""
        }
    }
}

struct LedgerState {
    var filters = LedgerFilters()
    var search = ""
    var selection: Entry.ID?
    var sort = [KeyPathComparator(\Entry.day, order: .reverse)]

    func includes(_ entry: Entry) -> Bool { filters.includes(entry) && entry.matches(search.trimmed) }
}

struct WorkspaceView: View {
    let store: SessionStore
    @ViewState private var section: WorkspaceSection? = .dashboard
    @ViewState private var columnVisibility: NavigationSplitViewVisibility = .automatic
    @ViewState private var dashboardRange: DashboardRange = .thisMonth
    @ViewState private var ledgerStates: [LedgerKind: LedgerState] = [:]
    @ViewState private var categoryKind: LedgerKind = .expense
    @ViewState private var categorySelection: Int?
    @ViewState private var showsInspector = false
    @ViewState private var presentsSearch = false
    @ViewState private var sheet: Sheet?
    @ViewState private var deletion: Deletion?

    private enum Sheet: Identifiable {
        case entry(LedgerKind, Entry?), category(LedgerKind), filters(LedgerKind), range
        var id: String {
            switch self {
            case let .entry(kind, entry): "\(kind.rawValue)-\(entry?.id.value ?? 0)"
            case let .category(kind): "category-\(kind.rawValue)"
            case let .filters(kind): "filters-\(kind.rawValue)"
            case .range: "range"
            }
        }
    }

    private enum Deletion {
        case entry(Entry), category(Category, LedgerKind)
        var title: String {
            switch self {
            case let .entry(entry): "Delete this \(entry.kind.singular.lowercased())?"
            case .category: "Delete this category?"
            }
        }
        var name: String {
            switch self {
            case let .entry(entry): entry.title ?? "\(entry.categoryName) · \(Money.display(entry.amount))"
            case let .category(category, _): category.name
            }
        }
    }

    private var currentSection: WorkspaceSection { section ?? .dashboard }
    private var kind: LedgerKind? { currentSection.ledger }
    private var canInteract: Bool { store.data != nil && !store.isBusy && sheet == nil && deletion == nil }
    private var selectedEntry: Entry? {
        guard let kind, let state = ledgerStates[kind], let id = state.selection else { return nil }
        return store.data?.entries(for: kind).first { $0.id == id && state.includes($0) }
    }
    private var selectedCategory: Category? {
        store.data?.categories(for: categoryKind).first { $0.id == categorySelection }
    }
    private var addTitle: String? {
        if let kind { return "New \(kind.singular)" }
        if currentSection == .categories { return "New Category" }
        return nil
    }

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            List(selection: $section) {
                ForEach(WorkspaceSection.allCases) { item in
                    Label(item.rawValue, systemImage: item.symbol).tag(item)
                }
            }
            .listStyle(.sidebar)
            .navigationSplitViewColumnWidth(min: 180, ideal: 216, max: 275)
            .accessibilityLabel("MoneyFlow sections")
        } detail: {
            detail
                .navigationTitle(currentSection.rawValue)
                .toolbar { toolbar }
                .inspector(isPresented: inspectorBinding) {
                    EntryInspector(entry: selectedEntry, edit: { perform(.edit) }, delete: { perform(.delete) },
                                   actionsEnabled: canInteract)
                        .inspectorColumnWidth(min: 205, ideal: 254, max: 340)
                }
        }
        .navigationSplitViewStyle(.balanced)
        .focusedSceneValue(\.workspaceActions, actions)
        .task(id: currentSection) { presentsSearch = false }
        .sheet(item: $sheet) { selected in sheetContent(selected) }
        .alert(deletion?.title ?? "Delete record?", isPresented: Binding(
            get: { deletion != nil }, set: { if !$0 { deletion = nil } }
        ), presenting: deletion) { item in
            Button("Delete", role: .destructive) { delete(item) }
            Button("Cancel", role: .cancel) { deletion = nil }
        } message: { item in Text(item.name) }
        .alert("MoneyFlow couldn’t complete the action", isPresented: Binding(
            get: { store.actionError != nil }, set: { if !$0 { store.actionError = nil } }
        )) {
            Button("OK") { store.actionError = nil }
        } message: { Text(store.actionError ?? "") }
    }

    @ViewBuilder private var detail: some View {
        if let data = store.data {
            VStack(spacing: 0) {
                if let error = store.refreshError {
                    HStack {
                        InlineError(message: "Showing previously loaded data. \(error)")
                        Spacer()
                        Button("Retry") { Task { await store.refresh() } }
                    }
                    .padding(.horizontal, 20).padding(.vertical, 8)
                    Divider()
                }
                switch currentSection {
                case .dashboard:
                    DashboardView(data: data, range: dashboardRange, openEntry: openEntry)
                case .expenses, .income:
                    if let kind {
                        let state = ledgerBinding(kind)
                        LedgerView(kind: kind, entries: data.entries(for: kind), state: state,
                                   canEdit: canInteract, add: { perform(.add) }, edit: { sheet = .entry(kind, $0) },
                                   inspect: { openEntry($0) }, delete: { deletion = .entry($0) })
                            .searchable(text: state.search, isPresented: $presentsSearch, placement: .toolbar,
                                        prompt: "Search \(kind.title)")
                    }
                case .categories:
                    categoriesView(data)
                }
            }
        } else if let error = store.refreshError {
            ContentUnavailableView {
                Label("Couldn’t Load MoneyFlow", systemImage: "exclamationmark.icloud")
            } description: {
                Text(error)
            } actions: {
                Button("Try Again") { Task { await store.refresh() } }
            }
        } else {
            ProgressView("Loading Account…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func categoriesView(_ data: AccountData) -> some View {
        let categories = data.categories(for: categoryKind)
        return VStack(spacing: 0) {
            HStack {
                Picker("Category Type", selection: $categoryKind) {
                    ForEach(LedgerKind.allCases) { Text($0.singular).tag($0) }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .fixedSize()
                Spacer()
                Text("\(categories.count) categories").foregroundStyle(.secondary).monospacedDigit()
            }
            .padding(.horizontal, 22).padding(.vertical, 12)
            Divider()
            if categories.isEmpty {
                ContentUnavailableView {
                    Label("No \(categoryKind.singular) Categories", systemImage: "tag")
                } actions: {
                    Button("New Category…") { perform(.add) }.disabled(!canInteract)
                }
            } else {
                List(categories, selection: $categorySelection) { category in
                    Label(category.name, systemImage: "tag")
                        .lineLimit(1)
                        .help(category.name)
                        .tag(category.id)
                }
                .listStyle(.inset(alternatesRowBackgrounds: true))
                .contextMenu(forSelectionType: Int.self) { ids in
                    if let category = categories.first(where: { ids.contains($0.id) }), ids.count == 1 {
                        Button("Delete Category…", role: .destructive) { deletion = .category(category, categoryKind) }
                            .disabled(!canInteract)
                    }
                }
            }
        }
        .onChange(of: categoryKind) { categorySelection = nil }
    }

    @ToolbarContentBuilder private var toolbar: some ToolbarContent {
        if currentSection == .dashboard, store.data != nil {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    ForEach(DashboardPreset.allCases) { preset in
                        Button {
                            dashboardRange = .preset(preset)
                        } label: {
                            if dashboardRange == .preset(preset) { Label(preset.rawValue, systemImage: "checkmark") }
                            else { Text(preset.rawValue) }
                        }
                    }
                    Divider()
                    Button("Custom Range…") { perform(.range) }
                } label: { Label("Date Range", systemImage: "calendar") }
                .help("Choose a dashboard date range")
            }
        }
        ToolbarItem(placement: .primaryAction) {
            Button { Task { await store.refresh() } } label: { Label("Refresh", systemImage: "arrow.clockwise") }
                .disabled(store.isBusy)
                .help(store.lastUpdated.map { "Last refreshed \($0.formatted(.dateTime.hour().minute()))" } ?? "Refresh account data")
        }
        if let kind, store.data != nil {
            ToolbarItem(placement: .primaryAction) {
                let filtered = ledgerStates[kind]?.filters.isActive == true
                Button { perform(.filters) } label: {
                    Label("Filters", systemImage: filtered ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
                .help(filtered ? "Edit active filters" : "Filter \(kind.title.lowercased())")
            }
            ToolbarItem(placement: .primaryAction) {
                Button { perform(.inspector) } label: { Label("Inspector", systemImage: "sidebar.right") }
                    .help(showsInspector ? "Hide inspector" : "Show inspector")
            }
        }
        if let addTitle {
            ToolbarItem(placement: .primaryAction) {
                Button { perform(.add) } label: { Label(addTitle, systemImage: "plus") }
                    .disabled(!canInteract)
                    .help(addTitle)
            }
        }
    }

    private var inspectorBinding: Binding<Bool> {
        Binding(get: { kind != nil && showsInspector }, set: { showsInspector = $0 })
    }

    private var actions: WorkspaceActions {
        WorkspaceActions(addTitle: canInteract ? addTitle : nil,
                         canEdit: canInteract && selectedEntry != nil,
                         canDelete: canInteract && (selectedEntry != nil || (currentSection == .categories && selectedCategory != nil)),
                         canSearch: kind != nil && store.data != nil, showsFilters: kind != nil && store.data != nil,
                         showsRange: currentSection == .dashboard && store.data != nil,
                         inspectorIsVisible: kind != nil && showsInspector, canInspect: kind != nil && store.data != nil,
                         perform: perform)
    }

    private func ledgerBinding(_ kind: LedgerKind) -> Binding<LedgerState> {
        Binding(get: { ledgerStates[kind, default: LedgerState()] }, set: { ledgerStates[kind] = $0 })
    }

    private func perform(_ command: WorkspaceCommand) {
        switch command {
        case .add:
            guard canInteract else { return }
            if let kind { sheet = .entry(kind, nil) }
            else if currentSection == .categories { sheet = .category(categoryKind) }
        case .edit:
            if canInteract, let entry = selectedEntry { sheet = .entry(entry.kind, entry) }
        case .delete:
            guard canInteract else { return }
            if let entry = selectedEntry { deletion = .entry(entry) }
            else if currentSection == .categories, let category = selectedCategory {
                deletion = .category(category, categoryKind)
            }
        case .search:
            if kind != nil { presentsSearch = true }
        case .filters:
            if let kind { sheet = .filters(kind) }
        case .range:
            if currentSection == .dashboard { sheet = .range }
        case .inspector:
            if kind != nil { showsInspector.toggle() }
        }
    }

    private func openEntry(_ entry: Entry) {
        var state = ledgerStates[entry.kind, default: LedgerState()]
        if !state.includes(entry) { state.filters = .init(); state.search = "" }
        state.selection = entry.id
        ledgerStates[entry.kind] = state
        section = entry.kind == .expense ? .expenses : .income
        showsInspector = true
    }

    @ViewBuilder private func sheetContent(_ sheet: Sheet) -> some View {
        switch sheet {
        case let .entry(kind, entry): EntryEditor(store: store, kind: kind, entry: entry)
        case let .category(kind): CategoryEditor(store: store, kind: kind)
        case let .filters(kind):
            let state = ledgerBinding(kind)
            let entries = store.data?.entries(for: kind) ?? []
            let otherMethods = Set(entries.compactMap { $0.method?.nilIfBlank }).subtracting(kind.methods)
            FilterEditor(kind: kind, categories: store.data?.categories(for: kind) ?? [],
                         methods: kind.methods + otherMethods.sorted(), filters: state.wrappedValue.filters) {
                state.wrappedValue.filters = $0
            }
        case .range:
            FilterEditor(filters: dashboardRange.filters()) { filters in
                dashboardRange = filters.isActive ? .custom(filters) : .preset(.all)
            }
        }
    }

    private func delete(_ target: Deletion) {
        deletion = nil
        Task {
            do {
                switch target {
                case let .entry(entry):
                    try await store.deleteEntry(entry.id)
                    if ledgerStates[entry.kind]?.selection == entry.id { ledgerStates[entry.kind]?.selection = nil }
                case let .category(category, kind):
                    try await store.deleteCategory(category.id, kind: kind)
                    if categoryKind == kind && categorySelection == category.id { categorySelection = nil }
                    if ledgerStates[kind]?.filters.categoryID == category.id { ledgerStates[kind]?.filters.categoryID = nil }
                }
            } catch APIError.expired {
                // The session store returns to sign-in.
            } catch {
                if case let .category(category, kind) = target, case APIError.response(500, _) = error,
                   store.data?.entries(for: kind).contains(where: { $0.categoryID == category.id }) == true {
                    store.actionError = "This category is still used by transactions. Move those transactions to another category before deleting it."
                } else {
                    store.actionError = error.localizedDescription
                }
            }
        }
    }
}
