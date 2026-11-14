from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from pathlib import Path

ROOT = Path(__file__).parent
OUT = ROOT / 'Business_Manager_Project_Report.docx'

doc = Document()
sec = doc.sections[0]
sec.top_margin, sec.bottom_margin = Inches(.75), Inches(.7)
sec.left_margin, sec.right_margin = Inches(.8), Inches(.8)

styles = doc.styles
styles['Normal'].font.name = 'Aptos'; styles['Normal']._element.rPr.rFonts.set(qn('w:eastAsia'), 'Aptos')
styles['Normal'].font.size = Pt(10.5)
styles['Normal'].paragraph_format.space_after = Pt(6)
for name, size in [('Title', 25), ('Heading 1', 16), ('Heading 2', 12.5), ('Heading 3', 11)]:
    s=styles[name]; s.font.name='Aptos Display' if name=='Title' else 'Aptos'; s._element.rPr.rFonts.set(qn('w:eastAsia'), s.font.name)
    s.font.size=Pt(size); s.font.bold=True; s.font.color.rgb=RGBColor(0,0,0)
    s.paragraph_format.space_before=Pt(12 if name!='Title' else 0); s.paragraph_format.space_after=Pt(6)

def shade(cell, fill):
    tcPr=cell._tc.get_or_add_tcPr(); shd=OxmlElement('w:shd'); shd.set(qn('w:fill'), fill); tcPr.append(shd)
def borders(table):
    tblPr=table._tbl.tblPr; el=OxmlElement('w:tblBorders')
    for edge in ('top','left','bottom','right','insideH','insideV'):
        e=OxmlElement('w:'+edge); e.set(qn('w:val'),'single'); e.set(qn('w:sz'),'4'); e.set(qn('w:color'),'D9D9D9'); el.append(e)
    tblPr.append(el)
def table(headers, rows, widths=None):
    t=doc.add_table(rows=1, cols=len(headers)); t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.style='Table Grid'; borders(t)
    for i,h in enumerate(headers):
        c=t.rows[0].cells[i]; c.text=h; shade(c,'1F4E78'); c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
        for r in c.paragraphs[0].runs: r.font.bold=True; r.font.color.rgb=RGBColor(255,255,255); r.font.size=Pt(9)
    for ri,row in enumerate(rows):
        cells=t.add_row().cells
        for i,v in enumerate(row):
            cells[i].text=str(v); cells[i].vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            if ri%2==1: shade(cells[i],'F3F7FA')
            for p in cells[i].paragraphs:
                for r in p.runs: r.font.size=Pt(8.5)
    if widths:
        for row in t.rows:
            for i,w in enumerate(widths): row.cells[i].width=Inches(w)
    doc.add_paragraph('')
    return t
def p(text='', boldlead=None):
    para=doc.add_paragraph()
    if boldlead and text.startswith(boldlead):
        r=para.add_run(boldlead); r.bold=True; para.add_run(text[len(boldlead):])
    else: para.add_run(text)
    return para
def bullets(items):
    for x in items: doc.add_paragraph(x, style='List Bullet')
def h(text, level=1): doc.add_heading(text, level)
def page(): doc.add_page_break()
def fig(path, caption):
    if path.exists():
        doc.add_picture(str(path), width=Inches(6.5))
        q=doc.add_paragraph(caption); q.alignment=WD_ALIGN_PARAGRAPH.CENTER
        for r in q.runs: r.italic=True; r.font.size=Pt(9)

# Cover
doc.add_paragraph('\n\n\n')
t=doc.add_paragraph(); t.style='Title'; t.alignment=WD_ALIGN_PARAGRAPH.CENTER; t.add_run('Business Manager')
q=doc.add_paragraph('Academic Project Report'); q.alignment=WD_ALIGN_PARAGRAPH.CENTER
for r in q.runs: r.bold=True; r.font.size=Pt(16)
doc.add_paragraph('\n')
for label, value in [('Technology Used','Java 17, Java Swing, Maven, MySQL 8, JDBC, FlatLaf, JFreeChart'),('Submitted by','[Student Name]'),('Course and Department','[Course Name and Department]'),('Institution','[Institution Name]'),('Academic Year','[Academic Year]')]:
    q=doc.add_paragraph(); q.alignment=WD_ALIGN_PARAGRAPH.CENTER; q.add_run(label + ': ').bold=True; q.add_run(value)
doc.add_paragraph('\n\n')
q=doc.add_paragraph('Submitted in partial fulfillment of the requirements for the award of the degree or diploma specified by the institution.'); q.alignment=WD_ALIGN_PARAGRAPH.CENTER
page()

h('2 Certificate')
p('This is to certify that the project report entitled “Business Manager” is a bona fide record of work carried out by [Student Name], bearing [Roll Number], under my guidance and supervision in partial fulfillment of the requirements of [Course Name] in the Department of [Department Name], [Institution Name], during the academic year [Academic Year].')
p('The work presented in this report has not been submitted elsewhere for the award of any degree or diploma to the best of my knowledge.')
table(['Signature','Name and Designation'], [['Project Guide','[Guide Name and Designation]'],['Head of Department','[HOD Name and Designation]'],['Student','[Student Name]']], [2.5,4.0])
p('Date: [Date]    Place: [Place]')
h('3 Declaration')
p('I, [Student Name], declare that this project report entitled “Business Manager” is my original work completed under the guidance of [Guide Name]. The report is based on the implementation available in the supplied project workspace. Wherever work of others has been referred to, it has been acknowledged in the references section.')
p('Signature of Student: ____________________    Date: ____________________')
h('4 Acknowledgement')
p('I express my sincere gratitude to my project guide, the Head of the Department, and the faculty members of [Institution Name] for their guidance and support. I thank my classmates, friends, and family for their encouragement during the development and documentation of this Java-based project. I also acknowledge the open-source Java ecosystem and the official documentation of the technologies used in this implementation.')
page()

h('Table of Contents')
for i,x in enumerate(['Abstract','Introduction','Problem Statement','Objectives','Scope of the Project','Existing System','Proposed System','Technology Stack','System Requirements','System Architecture','Modules of the System','Functional Requirements','Non Functional Requirements','Database Design','Detailed System Workflow','User Interface Design','Implementation Details','Code Structure','Testing','Screenshots','Security Features','Advantages','Limitations','Future Enhancements','Conclusion','References','Appendix','Project Analysis Summary'],5):
    p(f'{i}. {x}')
page()

h('5 Abstract')
p('Business Manager is a desktop business operations application implemented in Java. It provides a Swing-based workspace for registering a business, authenticating users, maintaining master records, billing sales, recording purchases, monitoring inventory, settling balances, tracking expenses, and viewing reports and analytics. The source code supports both SMALL and LARGE business modes. LARGE mode additionally exposes branch and employee management and uses a multi-branch dashboard.')
p('The implementation uses Java 17 with Maven, Swing and FlatLaf for the user interface, MySQL 8 with JDBC for persistent storage, and JFreeChart for chart views. The application stores business-scoped data in a normalized MySQL schema and applies role-based module access checks through the service and navigation layers. Its primary users are owners, administrators, managers, cashiers, inventory managers, and staff operating a small or multi-branch business. The implementation also contains an in-memory demonstration mode for safe UI presentation when selected by configuration.')
h('6 Introduction')
p('Small and growing businesses often need a single desktop workspace for items, stock, customers, suppliers, invoices, purchases, expenses, and operational reporting. Maintaining these records separately can make billing slower and reduce the visibility of stock and outstanding balances. Business Manager addresses this need through an integrated Java Swing application whose modules operate on a shared business database.')
p('The application begins with a welcome and onboarding sequence, then presents a dashboard tailored to the selected business type. The user can navigate with a sidebar to point-of-sale billing, management directories, inventory-related work, reports, analytics, and settings. This design brings related operational activities into a single GUI rather than treating a sale, stock change, payment, or report as an independent manual activity.')
h('7 Problem Statement')
p('The implementation addresses the operational problem of maintaining business records and transactions across multiple activities: product cataloguing, supplier and customer records, inventory monitoring, purchase replenishment, point-of-sale billing, payment settlement, expense logging, and performance reporting. For larger businesses, the same problem extends to branch-specific inventory, branch comparisons, and employee records.')
h('8 Objectives')
bullets(['Provide account creation, business registration, business-type selection, and authenticated access to a single desktop workspace.','Maintain business-scoped master records for categories, products, customers, suppliers, branches, and employees where applicable.','Support POS sale creation, invoice history, purchase recording, stock updates, and payment status handling.','Provide low-stock visibility, dashboard metrics, filtered reports, and chart-based analytics.','Restrict modules and write operations according to the active user role.','Persist production data in the project-defined MySQL schema while retaining a demo-mode path for UI demonstration.'])
h('9 Scope of the Project')
p('The current scope is a local Java desktop application connected to MySQL. It covers the operations represented by the UI panels and the database schema: business setup; users; products; categories; supplier and customer directories; sales and sale items; purchases and purchase items; branch inventory; payments; expenses; employees; reports; dashboards; and analytics. A LARGE business receives branch and employee panels, whereas a SMALL business receives a simplified dashboard. The project does not provide a web, mobile, cloud synchronization, remote multi-user deployment, or API layer identifiable from the available files.')
h('10 Existing System')
p('The source code does not include a separate legacy system. The appropriate existing-system comparison is therefore a conventional manual or fragmented desktop record-keeping process: product and supplier details held separately, stock quantities updated after transactions, invoices checked from paper or isolated files, and business summaries assembled manually. This is an inferred operational context, not a separately implemented module.')
h('11 Proposed System')
p('The proposed and implemented system is a centralized desktop workspace. Swing panels collect user input, services enforce module-level checks, DAO classes issue JDBC operations, and MySQL stores the resulting records. Sale and purchase creation combine document, line-item, inventory, and payment-related processing. Dashboards, reports, and JFreeChart visualizations then query the same persisted data for operational insight.')
page()

h('12 Technology Stack')
table(['Area','Technology found','Evidence in project'],[
['Programming language','Java 17','pom.xml compiler release and source/target settings'],['GUI framework','Java Swing','JFrame, JPanel, CardLayout, JTable, JDialog, JOptionPane throughout ui package'],['Look and feel','FlatLaf 3.6.1 and FlatLaf Extras 3.6.1','Maven dependencies'],['Database','MySQL 8.0+; InnoDB; utf8mb4','database README and schema.sql'],['Connectivity','JDBC via mysql-connector-j 9.4.0','DBConnection and Maven dependency'],['Charts','JFreeChart 1.5.6','AnalyticsDAO datasets and Maven dependency'],['Build tool','Apache Maven','pom.xml with compiler and Surefire plugins'],['Testing','JUnit Jupiter 5.11.4','Maven dependency and src/test/java'],['IDE','Not identifiable from the available project files','No IDE metadata is required by the build'],['Other','Java Print Service and custom PDF writer','InvoicePrinter']])
h('13 System Requirements')
h('13.1 Hardware Requirements',2)
p('The project does not specify a formal hardware baseline. A reasonable deployment baseline is a 64-bit desktop or laptop with a dual-core processor, 4 GB RAM, approximately 500 MB of free storage for the application, dependencies, logs, and generated invoice PDFs, and a display capable of comfortably showing desktop tables and forms. Printing requires an installed system print service if physical output is needed.')
h('13.2 Software Requirements',2)
table(['Requirement','Identifiable requirement or status'],[['Operating system','Not restricted in the source; Java Swing and Maven support common desktop operating systems.'],['Java','Java 17 required by Maven compiler configuration.'],['Database server','MySQL 8.0+ identified by database README.'],['Database driver','MySQL Connector/J 9.4.0.'],['Build tool','Maven.'],['Runtime libraries','FlatLaf 3.6.1, FlatLaf Extras 3.6.1, JFreeChart 1.5.6.'],['IDE','Not identifiable from available project files.']])
h('14 System Architecture')
p('The code follows a layered desktop architecture. The presentation layer is the Swing ui package. MainFrame owns the top-level JFrame and CardLayout; it switches between onboarding cards and the post-login workspace. UI panels construct forms, tables, dialogs, navigation controls, and event listeners. The service layer exposes application operations and uses AccessControl before most data operations. The DAO layer contains SQL/JDBC work and maps ResultSet records into model classes. DBConnection reads properties and environment overrides, while DatabaseInitializer checks or creates the database, applies schema.sql when tables are absent, and seeds the required administrator account.')
p('The application flow is initiated on the Swing event dispatch thread by Main.main. Database availability is checked before ordinary use. Authentication begins a UserSession containing the active User and Business. MainFrame uses the business type to load the appropriate dashboard and, for LARGE businesses, branch and employee modules. Data entered through dialogs reaches service methods and DAOs; query results return through models to JTable and dashboard/chart views.')
doc.add_paragraph('''+--------------------+       events / model data       +---------------------+
| Swing UI Panels    | <----------------------------> | Services + Security |
| MainFrame Card UI  |                                 | AccessControl       |
+---------+----------+                                 +----------+----------+
          |                                                        |
          | JDBC calls via DAOs                                    | authorization
          v                                                        v
+--------------------+      SQL / ResultSet       +---------------------+
| DAO classes        | <------------------------> | MySQL business_manager|
| DBConnection       |                            | 16-table schema      |
+--------------------+                            +---------------------+''')
page()

h('15 Modules of the System')
table(['Module','Purpose and main classes','User interaction and data behavior'],[
['Startup and onboarding','Main, MainFrame, WelcomePanel, CreateAccountPanel, BusinessInfoPanel, BusinessTypePanel, AuthService','Welcome, create-account, business-information, and business-type cards establish a user and business before opening the workspace.'],
['Authentication and session','LoginPanel, AuthService, UserDAO, PasswordHasher, UserSession, AccessControl','A user signs in by username/email and password. The active user/business are retained in UserSession; logout clears it.'],
['Dashboard and analytics','SmallBusinessDashboardPanel, LargeBusinessDashboardPanel, AnalyticsPanel, AnalyticsService, AnalyticsDAO','Shows metrics, sales/purchase trends, top products, category breakdowns, recent sales, activity, and branch views for large businesses.'],
['POS and sales','POSBillingPanel, SalesHistoryPanel, SaleService, SaleDAO, PosCalculationEngine, CouponService, InvoicePrinter','Builds a cart, calculates discounts/tax/payment values, records a sale and sale items, adjusts stock, shows invoice details, and supports preview/print/PDF output.'],
['Products, categories, inventory','ProductManagementPanel, CategoryManagementPanel, InventoryManagementPanel, ProductService, CategoryService, ProductDAO, CategoryDAO','Supports product/category CRUD, SKU checking, product attributes, stock quantities, low-stock visibility, and branch inventory where applicable.'],
['Purchases and suppliers','PurchaseManagementPanel, SupplierManagementPanel, PurchaseService, SupplierService, PurchaseDAO, SupplierDAO','Maintains supplier profiles and records purchase orders with product, quantity, and unit price; purchases replenish inventory.'],
['Customers and payments','CustomerManagementPanel, PaymentManagementPanel, CustomerService, PaymentService, CustomerDAO, PaymentDAO','Maintains customer profiles, lists outstanding sale/purchase balances, and records settlements.'],
['Expenses and reporting','ExpenseManagementPanel, ReportsPanel, ExpenseService, ReportService, ExpenseDAO, ReportDAO','Logs expenses and generates sales, purchase, inventory, expense, profit/loss, and executive-summary report data with filters.'],
['Large-business administration','BranchManagementPanel, EmployeeManagementPanel, BranchService, EmployeeService, BranchDAO, EmployeeDAO','Available only in LARGE-business workspace setup; maintains branch and employee data.'],
['Settings and configuration','SettingsPanel, DBConnection, DatabaseInitializer, DemoModeConfig','Provides password change, profile/system information, database connection test, and logout; configuration reads db.properties and environment overrides.']],[1.2,2.35,3.0])
h('16 Functional Requirements')
bullets(['The system shall display welcome, login, account creation, business-information, business-type, and main-application screens through CardLayout.','The system shall register a business and user, authenticate an active user, and clear the session on confirmed logout.','The system shall load different dashboards for SMALL and LARGE business types and only add branch/employee panels for LARGE businesses.','The system shall create, read, update, and delete categories, products, customers, suppliers, branches, employees, and expenses through the associated panels/services/DAOs.','The system shall create sales with item lines, totals, payment state, and inventory effects; it shall maintain sales history and invoice detail.','The system shall create purchases with item lines and inventory replenishment.','The system shall surface low-stock products and support dashboard/report/analytics queries.','The system shall filter reports by available UI criteria including report type, date range, category, customer, supplier, branch, and payment status.','The system shall enforce module access and write permission checks through AccessControl before protected service operations.','The system shall support invoice preview/printing and a PDF destination workflow through InvoicePrinter.'])
h('17 Non Functional Requirements')
table(['Area','Implemented evidence','Boundary or recommendation'],[['Usability','Dedicated Swing forms, navigation sidebar, dialogs, tables, filters, messages, and FlatLaf styling.','Usability evaluation with target users is not evidenced.'],['Reliability','Checked SQL exceptions, connection diagnostics, initialization checks, and a default uncaught-exception dialog on the event dispatch thread.','No evidence of backup, retry queue, or disaster recovery.'],['Security','Salted password hashing, active-user state, role checks, prepared statements in DAO code, database user described as dedicated in database README.','Password policy, account lockout, audit immutability, and encryption at rest are not identifiable.'],['Maintainability','Packages separate ui, service, dao, model, security, database, enums, theme, and tests; Maven centralizes dependencies.','No formal API documentation or dependency-injection framework identified.'],['Performance','JDBC queries and table-based presentation are suitable for ordinary desktop usage; login timeout is set to five seconds.','Load-testing evidence and pagination strategy are not identifiable.'],['Scalability','Schema includes business and branch dimensions.','No distributed deployment, server API, or concurrency/load evidence is present.']])
page()

h('18 Database Design')
p('The implementation targets a MySQL 8.0+ database named business_manager. The database README states that it uses InnoDB and utf8mb4. DBConnection loads db.properties and accepts BUSINESS_MANAGER_DB_URL, BUSINESS_MANAGER_DB_USER, and BUSINESS_MANAGER_DB_PASS environment overrides. DatabaseInitializer checks table availability and runs database/schema.sql if the users table is missing. The bootstrap script creates a dedicated business_manager_app@localhost account; its exact grants are defined in database/setup.sql.')
table(['Table','Key columns and implementation purpose'],[
['businesses','id PK; name; type SMALL/LARGE; registration_number; tax_id; contact and address fields.'],['branches','id PK; business_id FK; name; code; is_main; active; unique business_id + code.'],['users','id PK; business_id FK; optional branch_id FK; username; email; password_hash; salt; role; active.'],['categories','id PK; business_id FK; name; description; unique business_id + name.'],['suppliers','id PK; business_id FK; contact, tax and active fields.'],['customers','id PK; business_id FK; contact, tax and active fields.'],['products','id PK; business_id FK; category_id/supplier_id FKs; SKU; prices; stock_quantity; min_stock; tax_rate; active.'],['branch_inventory','id PK; branch_id/product_id FKs; quantity; min_stock; unique branch_id + product_id.'],['sales and sale_items','Sales document with business/branch/user/customer FKs, invoice number, totals and payment state; item lines link to sale and product.'],['purchases and purchase_items','Purchase document with business/branch/supplier/user FKs, totals and payment state; item lines link to purchase and product.'],['inventory_transactions','business, branch, product, user, transaction_type, quantity, reference, notes and timestamp.'],['payments','business, party type/id, reference type/id, amount, payment method, notes and date.'],['expenses','business/branch/user FKs; category; title; amount; description; expense date.'],['employees','business/branch/user FKs; employee code; employee, position, department, salary, hire date, active.']],[1.35,5.4])
h('18.1 Relationships and CRUD',2)
p('A business is the root scope for branches, users, categories, suppliers, customers, products, sales, purchases, payments, expenses, employees, and inventory transactions. Products may reference a category and supplier. Sales and purchases each own item rows; sales can reference a customer, and purchases reference a supplier. branch_inventory represents a branch-product stock record. The schema uses ON DELETE CASCADE for several business-owned entities and ON DELETE SET NULL for optional associations. DAO classes implement CRUD methods for the principal master entities; SaleDAO and PurchaseDAO implement document creation with related rows and stock/payment handling.')
doc.add_paragraph('''businesses 1---* branches 1---* branch_inventory *---1 products
     |              |                                  |\
     |              +---* users                         | +---1 categories
     +---* customers                                    +---1 suppliers
     +---* sales 1---* sale_items *---1 products
     +---* purchases 1---* purchase_items *---1 products
     +---* payments     +---* expenses     +---* employees
     +---* inventory_transactions ---1 products''')
h('19 Detailed System Workflow')
p('Application start -> Main installs an uncaught exception handler -> SwingUtilities.invokeLater creates MainFrame -> MainFrame verifies database readiness with DatabaseInitializer -> Welcome card -> login or registration route -> authentication starts UserSession -> MainFrame builds sidebar and business-type-specific modules -> user selects a module -> AccessControl evaluates module permission -> UI gathers/filter inputs -> service applies access checks -> DAO executes JDBC SQL -> model/result data returns to Swing tables, dialogs, dashboards, reports, or charts.')
p('For a sale, the POS panel builds item selections and uses PosCalculationEngine/CouponService for calculation behavior. SaleService delegates protected creation to SaleDAO. The DAO creates the sale and item records, validates/normalizes sale input, updates relevant stock, and may create payment-related records. The sale is then available in history/dashboard/report queries; InvoicePrinter supports preview, print, and PDF-destination export. Purchases follow an analogous purchase document and replenishment flow through PurchaseService and PurchaseDAO.')
h('20 User Interface Design')
table(['Screen or panel','Purpose and controls evidenced in source'],[
['WelcomePanel','Entry panel with logo, Business Manager title/tagline, descriptive text, and actions to sign in or begin setup.'],['LoginPanel','Authentication form with keep-signed-in checkbox, forgot-password message, sign-in navigation, and setup link.'],['CreateAccountPanel','Registration fields, password strength meter, confirm-password field, inline error display, and continuation action.'],['BusinessInfoPanel and BusinessTypePanel','Business setup cards and controls for business details/type selection; SMALL and LARGE choices lead to workspace setup.'],['MainFrame and SidebarNav','JFrame containing CardLayout screens; left sidebar navigates dashboard, POS, data-management, reporting, analytics, settings, and logout.'],['Dashboards','JPanels with labels, filter combos, refresh/retry buttons, metric cards, JTables, chart cards, activity feed; large dashboard adds branch filtering.'],['Management panels','Product/category/customer/supplier/branch/employee panels use search fields, JTables, add/edit/delete buttons, modal JDialogs, text fields, combo boxes, check boxes, spinners and scroll panes.'],['POSBillingPanel','POS billing interface; actual user operations include product/cart processing, totals and payment; report screenshots identify the POS screen.'],['ReportsPanel','Report-type JComboBox, export action, JTable, date spinners, category/customer/supplier/branch/status filters and apply/reset actions.'],['SettingsPanel','JPasswordFields and update-password button; labels for profile/system details; database test and logout buttons.']],[1.6,5.15])
page()

h('21 Implementation Details')
h('21.1 Object Oriented Structure',2)
p('Domain entities are represented by model classes such as Business, User, Product, Sale, SaleItem, Purchase, PurchaseItem, Customer, Supplier, Branch, Employee, Expense, Category, and DTOs for dashboard/report data. DAO classes map ResultSet values to these models. Service classes provide a domain-facing interface, for example ProductService.createProduct, SaleService.createSale, and PurchaseService.createPurchase. This separation keeps screen code from directly owning SQL statements.')
h('21.2 Event Driven Swing Programming',2)
p('The UI is event driven. Main.main starts the UI on SwingUtilities.invokeLater. Buttons and navigation controls register action listeners. MainFrame.showScreen and CardLayout handle onboarding transitions, while MainFrame.navigateToModule performs a role check before showing a main-content card. Dialog based forms collect entry data and refresh panel tables after operations. JOptionPane is used for confirmation, error, warning, and information messages.')
h('21.3 Database Connectivity and SQL',2)
p('DBConnection loads the resource db.properties, derives a server-level URL when required, loads com.mysql.cj.jdbc.Driver, applies DriverManager.setLoginTimeout(5), and returns JDBC Connections. DAO methods use JDBC ResultSet, PreparedStatement, Statement, generated keys, and SQL CRUD statements. DatabaseInitializer applies schema.sql if the database is reachable but the users table is absent. AnalyticsDAO creates JFreeChart DefaultCategoryDataset instances from query results, while ReportDAO generates rows for each supported report type.')
h('21.4 POS, Calculation, and Inventory Logic',2)
p('PosCalculationEngine and PosCalculationResult represent calculation logic, while CouponService keeps a synchronized in-memory registry of WELCOME10, FLAT500, FESTIVE15, and SUPER20 offers. CouponService supports percentage or flat discount definitions and minimum-purchase values. SaleDAO contains createSale, validateAndNormaliseSale, payment insertion, and branch-stock availability behavior. PurchaseDAO contains createPurchase and payment insertion behavior. ProductDAO contains stock update and branch-inventory support. The complete detail of every SQL calculation is implementation-specific; the report does not infer rules beyond these verified classes and methods.')
h('21.5 Error Handling and Validation',2)
p('The application uses SQL exception propagation through service methods and localized handling in UI panels. Main installs a default uncaught-exception handler which prints the exception and presents a readable Swing error dialog when the fault occurs on the event dispatch thread. DBConnection creates descriptive SQLException messages for missing driver, connection, and incomplete configuration cases. Input validation is present in panels and DAO/service routines, including authentication/setup form feedback, product SKU existence behavior, sale normalization, and database constraints. Exact validation messages vary by panel.')
h('21.6 Authentication and Authorization',2)
p('PasswordHasher is used by authentication and initial administrator seeding; the users table holds password_hash and salt rather than a plaintext password field. UserSession retains the current User and Business in synchronized static methods. AccessControl provides canAccessModule and requireWrite checks. MainFrame blocks navigation to modules without permission, and service operations such as product, sale, and purchase calls enforce access before invoking DAOs. Roles in the schema are ADMIN, OWNER, MANAGER, CASHIER, and INVENTORY_MANAGER.')
h('21.7 Invoice Output and File Handling',2)
p('InvoicePrinter implements Printable. It can show an invoice-preview JDialog, invoke a selected print service, and, when an explicit PDF destination is used with a PDF print service, write a self-contained PDF file. The implementation creates parent directories when needed and writes a simple PDF using Java NIO. No broad document-management or general file-upload feature was identified.')
h('22 Code Structure')
doc.add_paragraph('''com.businessmanager
|- Main                         application entry point
|- ui                           Swing panels and MainFrame
|- components, theme            sidebar, icons, visual styling
|- service                      authorization-aware operations
|- dao                          JDBC SQL and ResultSet mapping
|- model                        entities and dashboard/report DTOs
|- database                     DBConnection and DatabaseInitializer
|- security                     session, password hashing, access control
|- enums                        business, role, payment, expense, transaction values
|- demo                         in-memory demonstration data
|- utils                        invoice preview, printing and PDF writing''')
p('Key relationships are Main -> MainFrame -> UI panels; panels -> services -> DAOs -> DBConnection/MySQL; services and MainFrame -> AccessControl/UserSession; dashboards/analytics -> AnalyticsService/AnalyticsDAO -> JFreeChart datasets; POS/sales -> PosCalculationEngine, CouponService, SaleService/SaleDAO, and InvoicePrinter.')
h('23 Testing')
p('The repository includes JUnit tests in unit, service, security, integration, e2e, and demo packages. Named files include PosBillingEngineTest, DemoAuthenticationServiceTest, PasswordHasherTest, ServicePermissionTest, AccessControlTest, LogoutFeatureTest, MySqlIntegrationTest, DashboardRealDataTest, BranchInventoryPosTest, BackendCompleteAuditMatrixTest, StitchDashboardPosVerificationTest, and UI startup/manual-verification classes. A Maven test run was attempted during report preparation. Main-source compilation completed for 96 source files, but test compilation failed because DemoAssessmentFlowTest and DemoAuthenticationServiceTest reference com.businessmanager.service.DemoAuthenticationService, a class not present in the available main-source files. Therefore no JUnit tests executed in that run.')
table(['ID','Feature','Input','Expected result','Actual observed result','Status'],[
['TC-01','Password hashing','Password and generated salt','Hashing/verification behavior follows PasswordHasher test contract.','Not executed: test compilation blocked by missing DemoAuthenticationService.','Blocked'],['TC-02','Authentication','Valid/invalid user credentials','Demo/real authentication behavior follows DemoAuthenticationServiceTest.','Test compile failure: referenced DemoAuthenticationService is absent.','Blocked'],['TC-03','POS calculation','Cart, coupon, tax/payment values','Totals and billing logic follow PosBillingEngineTest contract.','Not executed because Maven test compilation failed first.','Blocked'],['TC-04','Authorization','Role and protected module/write operation','Unauthorized service/module use is rejected.','Not executed because Maven test compilation failed first.','Blocked'],['TC-05','Logout','Active session and logout command','Session is cleared and login screen is returned.','Not executed because Maven test compilation failed first.','Blocked'],['TC-06','Database integration','Configured MySQL database','Schema/data/branch/inventory/POS behavior follows integration-test contracts.','Not executed because Maven test compilation failed first.','Blocked'],['TC-07','GUI startup','Application Swing startup','UI panels initialize without identified startup failure.','Not executed because Maven test compilation failed first.','Blocked']],[.55,1.05,1.2,1.65,1.65,.7])
page()

h('24 Screenshots')
p('The following screenshots are actual PNG files available under scratch/renders in the project workspace. They are included as visual evidence of implemented screens, not fabricated illustrations.')
shots=[('Welcome.png','Figure 1. Welcome screen - landing page for Business Manager.'),('Login.png','Figure 2. Login screen - user authentication interface.'),('CreateAccount.png','Figure 3. Create Account screen - registration interface.'),('BusinessType.png','Figure 4. Business Type screen - SMALL/LARGE selection.'),('SmallDashboard.png','Figure 5. Small Business Dashboard - operational summary.'),('LargeDashboard.png','Figure 6. Large Business Dashboard - multi-branch summary.'),('POS.png','Figure 7. POS Billing interface - sales transaction workspace.'),('Products.png','Figure 8. Product Management - catalog and product record UI.'),('Inventory.png','Figure 9. Inventory Management - stock monitoring UI.'),('Customers.png','Figure 10. Customer Directory - customer record UI.'),('Suppliers.png','Figure 11. Supplier Directory - supplier record UI.'),('BusinessInfo.png','Figure 12. Business Information - onboarding/business profile UI.')]
for name,cap in shots:
    fig(ROOT/'scratch'/'renders'/name, cap)
    if name not in ('BusinessInfo.png',): page()
h('25 Security Features')
table(['Feature','Verified implementation'],[['Authentication','LoginPanel/AuthService/UserDAO use user credentials; the schema includes users with active state.'],['Password handling','PasswordHasher generates salt and hashes passwords; database stores password_hash and salt. DatabaseInitializer uses it for the required admin account.'],['Authorization','UserRole enum contains ADMIN, OWNER, MANAGER, CASHIER, and INVENTORY_MANAGER; AccessControl checks modules and writes.'],['Session lifecycle','UserSession stores active user/business with synchronized methods; MainFrame logout clears it after confirmation.'],['SQL safety','DAO code uses PreparedStatement for parameterized queries in many CRUD/transaction paths.'],['Database scope','database README documents a dedicated application database/user; setup.sql defines the bootstrap.'],['Not identified','No evidence of MFA, password-reset token delivery, lockout/rate limiting, TLS configuration, encrypted backup, or external identity provider.']])
h('26 Advantages')
bullets(['Integrated desktop handling of core business records and transactions.','Business-scoped relational schema with branch support for larger organizations.','Separate small and large business dashboard/workspace behavior.','Role-based navigation and service-side permission checks.','Stock-aware sales and inventory-replenishing purchase behavior.','Operational views through reports, JFreeChart analytics, low-stock lists, and recent activities.','Invoice preview, print, and destination-PDF support without a separate PDF dependency.','Maven build configuration and JUnit test structure support reproducible development.'])
h('27 Limitations')
bullets(['The configured db.properties file contains a local development password in plaintext; environment-variable overrides exist, but secret management is not otherwise implemented.','The application is a local desktop client; no web/mobile client, REST API, synchronization service, or cloud deployment is identified.','The documentation does not evidence database backups, migrations beyond initialization, centralized logging, or production monitoring.','Coupon definitions reside in the in-memory CouponService registry rather than a database table.','The database schema does not include a coupons table, and Coupon has no DAO; coupon administration is therefore not an implemented persisted module.','Payment party linkage uses party_type and party_id rather than declared foreign keys to separate customer/supplier tables, so referential integrity at that field pair is not enforced by schema FK constraints.','The project includes test sources, but this report does not assert a fresh full-suite execution result.','No formal accessibility, usability, concurrency, performance, or security penetration-test evidence is available.'])
h('28 Future Enhancements')
bullets(['Move database credentials out of committed properties into an environment/secret-management workflow and rotate exposed development credentials.','Add account-lockout, password-policy, reset-token, and audit-trail features.','Persist coupon definitions and provide a role-protected promotion-management screen.','Add backup/restore, schema migration tooling, logging, monitoring, and database-health diagnostics.','Provide export formats, notification workflows, barcode-scanner integration, and richer invoice templates if required by users.','Develop web/mobile or API clients only if multi-device and remote access become a requirement.','Add automated UI, load, accessibility, and database-transaction test coverage with documented CI execution.'])
h('29 Conclusion')
p('Business Manager is a Java 17 Swing and MySQL application that consolidates operational functions for a small or large business. Its implementation covers onboarding, authentication, role-aware navigation, product and party directories, inventory, POS sales, purchases, payments, expenses, reporting, analytics, dashboards, and settings. The project demonstrates a layered structure in which Swing panels use services, services use DAO classes and access control, and DAOs use JDBC against the business_manager schema. The design directly supports the stated operational problem of coordinated business records and transactions while leaving realistic security, deployment, and operational enhancements for future work.')
page()

h('30 References')
bullets(['Oracle. Java Platform, Standard Edition 17 Documentation. https://docs.oracle.com/en/java/javase/17/','Oracle. The Java Tutorials: Creating a GUI With Swing. https://docs.oracle.com/javase/tutorial/uiswing/','Oracle. JDBC Basics. https://docs.oracle.com/javase/tutorial/jdbc/basics/','Apache Maven. Maven Documentation. https://maven.apache.org/guides/','MySQL. MySQL 8.0 Reference Manual. https://dev.mysql.com/doc/refman/8.0/en/','FlatLaf Documentation. https://www.formdev.com/flatlaf/','JFreeChart. Project documentation. https://www.jfree.org/jfreechart/','Project source evidence: pom.xml, database/README.md, database/schema.sql, src/main/java, src/test/java, and src/main/resources in the supplied workspace.'])
h('31 Appendix')
h('31.1 Important Database Operations',2)
p('The schema uses CREATE TABLE definitions for 16 tables. Representative DAO operations include BranchDAO.create/update/delete; CategoryDAO.create/update/delete/hasAssociatedProducts; CustomerDAO.create/update/delete; SupplierDAO.create/update/delete; ProductDAO.create/update/delete/updateStock; EmployeeDAO.create/update/delete; ExpenseDAO.create/update/delete; SaleDAO.createSale; PurchaseDAO.createPurchase; PaymentDAO.settleSale and settlePurchase; and ReportDAO.generateReport.')
h('31.2 Configuration and Initialization',2)
p('db.properties configures db.url, db.user, and db.password. DBConnection supports BUSINESS_MANAGER_DB_URL, BUSINESS_MANAGER_DB_USER, and BUSINESS_MANAGER_DB_PASS environment-variable overrides. DatabaseInitializer first attempts the target database, attempts server-level database creation after MySQL error 1049, applies schema.sql when users does not exist, and ensures an administrator account if the email admin@businessmanager.com is not present. The seeded administrator credentials are defined in source; they should be changed before any real deployment.')
h('31.3 Major Class List',2)
table(['Package area','Representative classes'],[['Entry/database','Main, MainFrame, DBConnection, DatabaseInitializer'],['Security','AccessControl, PasswordHasher, UserSession'],['Services','AuthService, ProductService, SaleService, PurchaseService, PaymentService, ReportService, AnalyticsService, BranchService, EmployeeService, CustomerService, SupplierService, CategoryService, ExpenseService, CouponService'],['DAOs','UserDAO, BusinessDAO, ProductDAO, SaleDAO, PurchaseDAO, PaymentDAO, ReportDAO, AnalyticsDAO, BranchDAO, EmployeeDAO, CustomerDAO, SupplierDAO, CategoryDAO, ExpenseDAO'],['User interface','WelcomePanel, LoginPanel, CreateAccountPanel, BusinessInfoPanel, BusinessTypePanel, dashboards, POSBillingPanel, management panels, ReportsPanel, AnalyticsPanel, SettingsPanel'],['Models','Business, User, Product, Sale, SaleItem, Purchase, PurchaseItem, Customer, Supplier, Branch, Employee, Expense, Category and dashboard/report DTOs']])
h('32 Project Analysis Summary')
table(['Item','Analysis summary'],[['Project purpose','Centralize desktop business operations including setup, sales, stock, purchases, parties, expenses, payments, reporting, and analytics.'],['Main modules','Onboarding/authentication; dashboards; POS/sales; products/categories/inventory; suppliers/purchases; customers/payments; expenses/reports; analytics; branches/employees; settings.'],['Technologies','Java 17, Java Swing, Maven, MySQL 8, JDBC/MySQL Connector/J, FlatLaf, FlatLaf Extras, JFreeChart, JUnit Jupiter.'],['Database used','MySQL business_manager with 16 tables defined in database/schema.sql.'],['Major screens/forms','At least 20 panel classes in ui plus MainFrame; 12 actual project UI captures are identified in this report.'],['Major classes','Main/MainFrame; DBConnection/DatabaseInitializer; security classes; 14 DAO classes; 14 service classes; UI panels; model/DTO classes.'],['Major features','Registration/login, roles, CRUD directories, POS, sales history, purchase replenishment, stock, payment settlement, expense tracking, reports, analytics, dashboard variants, invoice output.'],['Main workflow','Start -> database readiness -> onboarding/login -> session -> dashboard/module navigation -> validation/authorization -> service/DAO -> MySQL -> visible result.'],['Important observations','Layered package structure and MySQL schema are well defined. Demo-mode code and JUnit test sources are present, but the current test compile is blocked by absent DemoAuthenticationService references. Sensitive local DB configuration and absent operational/security hardening are key boundaries.']],[1.7,5.05])

# footer page field approximation
for section in doc.sections:
    footer=section.footer.paragraphs[0]; footer.alignment=WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run('Business Manager Project Report | ')
    fld=OxmlElement('w:fldSimple'); fld.set(qn('w:instr'),'PAGE'); footer._p.append(fld)

doc.core_properties.title='Business Manager Academic Project Report'
doc.core_properties.subject='Reverse engineered report based on project implementation'
doc.save(OUT)
print(OUT)
