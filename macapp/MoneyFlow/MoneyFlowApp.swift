import SwiftUI

// Use the SwiftUI property wrapper with both the macOS 26 and later SDKs.
typealias ViewState<Value> = SwiftUI.State<Value>

@main
struct MoneyFlowApp: App {
    @ViewState private var store = SessionStore()
    @AppStorage("appearance") private var appearance = Appearance.system

    var body: some Scene {
        Window("MoneyFlow", id: "main") {
            Group {
                if store.isRestoring {
                    ProgressView("Opening MoneyFlow…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let session = store.sessionID {
                    WorkspaceView(store: store)
                        .id(session)
                        .task { await store.refresh() }
                } else {
                    SignInView(store: store)
                }
            }
            .frame(minWidth: 740, minHeight: 500)
            .preferredColorScheme(appearance.scheme)
            .task { store.restore() }
        }
        .defaultSize(width: 1100, height: 750)
        .windowToolbarStyle(.unified)
        .windowResizability(.contentMinSize)
        .commands {
            SidebarCommands()
            WorkspaceCommands(store: store)
        }

        Settings {
            SettingsView(store: store, appearance: $appearance)
                .preferredColorScheme(appearance.scheme)
        }
    }
}

enum Appearance: String, CaseIterable, Identifiable {
    case system = "System", light = "Light", dark = "Dark"
    var id: Self { self }
    var scheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

private struct SettingsView: View {
    let store: SessionStore
    @Binding var appearance: Appearance
    @ViewState private var confirmsSignOut = false

    var body: some View {
        Form {
            Section("General") {
                Picker("Appearance", selection: $appearance) {
                    ForEach(Appearance.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
            }
            Section("Account") {
                LabeledContent("Status", value: store.sessionID == nil ? "Signed Out" : "Signed In")
                if store.sessionID != nil {
                    Button("Sign Out…", role: .destructive) { confirmsSignOut = true }
                        .disabled(store.isSaving)
                }
            }
        }
        .formStyle(.grouped)
        .frame(width: 420, height: 275)
        .confirmationDialog("Sign out of MoneyFlow?", isPresented: $confirmsSignOut, titleVisibility: .visible) {
            Button("Sign Out", role: .destructive) { store.signOut() }
        }
    }
}

enum WorkspaceCommand {
    case add, edit, delete, search, filters, range, inspector
}

struct WorkspaceActions {
    let addTitle: String?
    let canEdit: Bool
    let canDelete: Bool
    let canSearch: Bool
    let showsFilters: Bool
    let showsRange: Bool
    let inspectorIsVisible: Bool
    let canInspect: Bool
    let perform: (WorkspaceCommand) -> Void
}

private struct WorkspaceActionsKey: FocusedValueKey {
    typealias Value = WorkspaceActions
}

extension FocusedValues {
    var workspaceActions: WorkspaceActions? {
        get { self[WorkspaceActionsKey.self] }
        set { self[WorkspaceActionsKey.self] = newValue }
    }
}

private struct WorkspaceCommands: Commands {
    let store: SessionStore
    @FocusedValue(\.workspaceActions) private var actions

    var body: some Commands {
        CommandGroup(replacing: .newItem) {
            Button((actions?.addTitle ?? "New Entry") + "…") { actions?.perform(.add) }
                .keyboardShortcut("n")
                .disabled(actions?.addTitle == nil)
        }
        CommandGroup(after: .textEditing) {
            Button("Find…") { actions?.perform(.search) }
                .keyboardShortcut("f")
                .disabled(actions?.canSearch != true)
        }
        CommandGroup(after: .toolbar) {
            Button("Refresh") { Task { await store.refresh() } }
                .keyboardShortcut("r")
                .disabled(actions == nil || store.sessionID == nil || store.isBusy)
            Button("Date Range…") { actions?.perform(.range) }
                .disabled(actions?.showsRange != true)
            Button("Filters…") { actions?.perform(.filters) }
                .keyboardShortcut("f", modifiers: [.command, .shift])
                .disabled(actions?.showsFilters != true)
            Button(actions?.inspectorIsVisible == true ? "Hide Inspector" : "Show Inspector") {
                actions?.perform(.inspector)
            }
            .keyboardShortcut("i", modifiers: [.command, .option])
            .disabled(actions?.canInspect != true)
        }
        CommandMenu("Records") {
            Button("Edit Selected Transaction…") { actions?.perform(.edit) }
                .keyboardShortcut("e")
                .disabled(actions?.canEdit != true)
            Button("Delete Selected Record…", role: .destructive) { actions?.perform(.delete) }
                .keyboardShortcut(.delete, modifiers: .command)
                .disabled(actions?.canDelete != true)
        }
        CommandGroup(before: .appTermination) {
            Button("Sign Out") { store.signOut() }
                .disabled(store.sessionID == nil || store.isSaving)
        }
    }
}

private struct SignInView: View {
    let store: SessionStore
    @ViewState private var isRegistering = false
    @ViewState private var isSubmitting = false
    @ViewState private var username = ""
    @ViewState private var password = ""
    @ViewState private var fullName = ""
    @ViewState private var email = ""
    @ViewState private var error: String?
    @FocusState private var focus: Field?

    private enum Field: Hashable { case name, username, email, password }

    var body: some View {
        VStack(spacing: 22) {
            VStack(spacing: 8) {
                Image(systemName: "chart.bar.xaxis")
                    .font(.system(size: 42, weight: .medium))
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                Text(isRegistering ? "Create a MoneyFlow account" : "Sign in to MoneyFlow")
                    .font(.title.bold())
                    .accessibilityAddTraits(.isHeader)
                if !isRegistering {
                    Text("Use the same account you use on the web.")
                        .foregroundStyle(.secondary)
                }
            }

            VStack(alignment: .leading, spacing: 10) {
                if isRegistering {
                    TextField("Full Name", text: $fullName, prompt: Text("Full name"))
                        .textContentType(.name)
                        .focused($focus, equals: .name)
                }
                TextField("Username", text: $username, prompt: Text("Username"))
                    .textContentType(.username)
                    .focused($focus, equals: .username)
                if isRegistering {
                    TextField("Email", text: $email, prompt: Text("Email address"))
                        .textContentType(.emailAddress)
                        .focused($focus, equals: .email)
                }
                SecureField("Password", text: $password, prompt: Text("Password"))
                    .textContentType(isRegistering ? .newPassword : .password)
                    .focused($focus, equals: .password)
                if let error {
                    InlineError(message: error)
                } else if let notice = store.signInNotice {
                    Text(notice).font(.callout).foregroundStyle(.secondary)
                }
            }
            .labelsHidden()
            .textFieldStyle(.roundedBorder)
            .controlSize(.large)
            .disabled(isSubmitting)

            HStack {
                Button(isRegistering ? "Back to Sign In" : "Create Account…") { switchForm() }
                    .disabled(isSubmitting)
                Spacer()
                if isSubmitting { ProgressView().controlSize(.small).accessibilityLabel("Contacting MoneyFlow") }
                Button(isRegistering ? "Create Account" : "Sign In") { submit() }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(isSubmitting)
            }
        }
        .frame(width: 340)
        .padding(30)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task { focus = .username }
    }

    private func switchForm() {
        isRegistering.toggle()
        error = nil
        password = ""
        store.clearSignInNotice()
        focus = isRegistering ? .name : .username
    }

    private func submit() {
        guard !isSubmitting else { return }
        let username = username
        guard !username.trimmed.isEmpty, !password.isEmpty else {
            error = "Enter your username and password."
            return
        }
        if isRegistering {
            let address = email.trimmed
            let emailParts = address.split(separator: "@", omittingEmptySubsequences: false)
            guard !fullName.trimmed.isEmpty, username.databaseLength <= 80, fullName.trimmed.databaseLength <= 120 else {
                error = "Enter a full name (up to 120 characters) and a username (up to 80 characters)."
                return
            }
            guard emailParts.count == 2, emailParts.allSatisfy({ !$0.isEmpty }),
                  address.databaseLength <= 255, !address.contains(where: \.isWhitespace) else {
                error = "Enter a valid email address."
                return
            }
        }
        isSubmitting = true
        error = nil
        store.clearSignInNotice()
        Task {
            defer { isSubmitting = false }
            do {
                if isRegistering {
                    try await store.register(.init(fullName: fullName.trimmed, userName: username,
                                                   email: email.trimmed, password: password))
                    isRegistering = false
                    password = ""
                    focus = .password
                } else {
                    try await store.signIn(.init(userName: username, password: password))
                    password = ""
                }
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct InlineError: View {
    let message: String
    var body: some View {
        Label(message, systemImage: "exclamationmark.circle")
            .font(.callout)
            .foregroundStyle(.red)
            .fixedSize(horizontal: false, vertical: true)
    }
}
