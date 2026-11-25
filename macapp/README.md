# MoneyFlow for macOS

## 1.0.0 · First Mac release

- Use your MoneyFlow account to access the same data as the web app. The app stores your session token in Keychain.
- Review spending, income, and net cash flow with charts, category and payment breakdowns, and recent activity.
- Add, edit, and delete transactions in searchable, sortable ledgers. Filter by date, amount, category, or payment method, and inspect individual records.
- Create and delete separate income and expense categories. Use native keyboard shortcuts, the toolbar, and system, light, or dark appearance.

## Build from source

Use macOS 26 or later and Xcode 26 or later. From the repository root, check out the release and open the project.

```sh
git switch --detach macos-v1.0.0
open macapp/MoneyFlow.xcodeproj
```

Select the shared **MoneyFlow** scheme and **My Mac**, then choose **Product → Run**. The project uses ad hoc signing for local builds and enables the App Sandbox with outbound networking. Sign in with your existing MoneyFlow username and password, or create an account in the app.

The workspace opens on Dashboard. Use the toolbar or the View menu to refresh, select dates, filter records or show the inspector. Command-F opens ledger search. Double-click a transaction to inspect it. Command-comma opens Settings.

## Release build

Run the following command from the repository root with Xcode selected as the active developer directory.

```sh
xcodebuild -project macapp/MoneyFlow.xcodeproj \
  -scheme MoneyFlow -configuration Release \
  -destination 'platform=macOS' -derivedDataPath macapp/.build/xcode build
```

The local app bundle is `macapp/.build/xcode/Build/Products/Release/MoneyFlow.app`. For a public download, archive the app in Xcode, sign it with Developer ID, and notarize it before uploading the packaged app to the GitHub release for `macos-v1.0.0`. [Apple notarization guide](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)

To publish the commits and source tag from `main`, run `git push --atomic origin main macos-v1.0.0`. Use the version notes above for the GitHub release description.

## API integration map

The Mac app connects to `https://moneyflow-czo7.onrender.com`. The web app configures its server through `VITE_API_URL`. Use `GET /alive` to check whether the API is responding.

| Method and path | Request | Successful response |
| --- | --- | --- |
| `POST /auth/register` | `fullName`, `userName`, `email`, `password` | `201` with `message` and `user_id`; the server seeds expense categories. Sign in separately. |
| `POST /auth/login` | `userName`, `password` | `200` with `token`, a five-day JWT |
| `GET /expenses` | Optional `min`, `max`, `category_id`, `payment_mode`, `from`, `to` | Array of `expense_id`, `title`, `amount`, `date`, `name`, `category_id`, `note`, `payment_mode` |
| `POST /expenses` | `title`, `amount`, `date`, `category_id`, `payment_mode`, optional `note`; API also accepts `group_id` | `201` with `message` and `expense_id` |
| `PATCH /expenses/:id` | `title`, `amount`, `date`, `category_id`, `note`; API also accepts `group_id` | `200` with `message` |
| `DELETE /expenses/:id` | Path ID | `200` with `message` |
| `GET /income` | Optional `min`, `max`, `category_id`, `credited_to`, `from`, `to` | Array of `income_id`, `amount`, `date`, `name`, `category_id`, `note`, `credited_to` |
| `POST /income` | `amount`, `date`, `category_id`, `credited_to`, optional `note` | `201` with `message` and `income_id` |
| `PATCH /income/:id` | `amount`, `date`, `category_id`, `credited_to`, `note` | `200` with `message` |
| `DELETE /income/:id` | Path ID | `200` with `message` |
| `GET /category` | Add `?type=income` for income categories | Array of category records with `id` and `name` |
| `POST /category` | `name`; add `?type=income` for income categories | `201` with `message` and `category_id` |
| `DELETE /category/:id` | Path ID; add `?type=income` for income categories | `200` with `message` |

- Protected calls send `Authorization: Bearer <token>`. Express verifies the JWT and scopes reads and writes to its user ID. The schema's category foreign keys also include the user ID. Sign-out removes the local token. There is no refresh, logout, account profile or token revocation endpoint.
- Errors use `error` or `message`, with `401` for invalid credentials or protected sessions, `400` for invalid input, `404` for missing records and `500` for database errors. A category assigned to a transaction is protected by a foreign key. The API supports creating and deleting categories.
- Expense payment values are `Credit Card`, `upi` and `cash`. Existing expenses can have a missing payment method. Expense payment method is set at creation and read-only during editing because `PATCH /expenses/:id` excludes `payment_mode`. Income `credited_to` is editable and accepts `Bank Account` and `Cash`. `group_id` has no web editor and is absent from the expense read response.
- The app displays INR and preserves the API's Gregorian `YYYY-MM-DD` calendar dates. Filtering is inclusive. Amounts have two decimal places in the database. Both create controllers require a nonzero amount, and existing zero amounts remain editable.
- The API returns complete lists ordered by descending date and ID. The app applies the same filters to its full snapshot and adds local search and table sorting. The API has no pagination, search or analytics routes. Net cash flow is the difference between recorded income and expenses for the chosen dates.
- Apple design guidance includes [Sidebars](https://developer.apple.com/design/human-interface-guidelines/sidebars), [Toolbars](https://developer.apple.com/design/human-interface-guidelines/toolbars), [Menu bar](https://developer.apple.com/design/human-interface-guidelines/the-menu-bar), [Layout](https://developer.apple.com/design/human-interface-guidelines/layout), [Typography](https://developer.apple.com/design/human-interface-guidelines/typography), [Color](https://developer.apple.com/design/human-interface-guidelines/color), [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility), [Adopting Liquid Glass](https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass), [SwiftUI](https://developer.apple.com/documentation/swiftui/).
