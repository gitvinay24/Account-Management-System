package com.businessmanager.demo;

import com.businessmanager.enums.ExpenseCategory;
import com.businessmanager.enums.PaymentMethod;
import com.businessmanager.enums.PaymentStatus;
import com.businessmanager.model.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Assessment-mode data provider. It intentionally lives beside, rather than inside,
 * the JDBC DAOs so production persistence remains untouched when demo mode is disabled.
 */
public final class DemoDataStore {
    public record PaymentEntry(int id, String partyType, int partyId, String referenceType,
                               int referenceId, BigDecimal amount, PaymentMethod method,
                               String notes, Timestamp date) { }

    private static final AtomicInteger IDS = new AtomicInteger(1000);
    private static final List<Category> categories = new ArrayList<>();
    private static final List<Product> products = new ArrayList<>();
    private static final List<Customer> customers = new ArrayList<>();
    private static final List<Supplier> suppliers = new ArrayList<>();
    private static final List<Branch> branches = new ArrayList<>();
    private static final List<Expense> expenses = new ArrayList<>();
    private static final List<Sale> sales = new ArrayList<>();
    private static final List<Purchase> purchases = new ArrayList<>();
    private static final List<PaymentEntry> payments = new ArrayList<>();

    private DemoDataStore() { }

    static { reset(); }

    public static synchronized void reset() {
        IDS.set(1000); categories.clear(); products.clear(); customers.clear(); suppliers.clear();
        branches.clear(); expenses.clear(); sales.clear(); purchases.clear(); payments.clear();
        category("Electronics", "Computers and electronic accessories");
        category("Grocery", "Everyday essentials");
        category("Stationery", "Office and school supplies");
        category("Office Equipment", "Office furniture and equipment");
        branch("Main Branch", "MAIN", true); branch("Jaipur Branch", "JPR", false);
        customer("Rahul Sharma", "rahul@example.com", "9876543210");
        customer("Priya Mehta", "priya@example.com", "9876543211");
        customer("Amit Verma", "amit@example.com", "9876543212");
        customer("Neha Gupta", "neha@example.com", "9876543213");
        supplier("ABC Distributors"); supplier("Sharma Wholesale"); supplier("Metro Suppliers");
        product("Laptop", "EL-1001", "Electronics", "ABC Distributors", "45000", "60000", 20, 5);
        product("Wireless Mouse", "EL-1002", "Electronics", "ABC Distributors", "600", "999", 45, 8);
        product("Keyboard", "EL-1003", "Electronics", "Sharma Wholesale", "1200", "1800", 32, 6);
        product("Monitor", "EL-1004", "Electronics", "Metro Suppliers", "11000", "15000", 18, 4);
        product("Printer", "OE-1001", "Office Equipment", "Metro Suppliers", "9000", "12500", 9, 3);
        product("Notebook", "ST-1001", "Stationery", "Sharma Wholesale", "35", "60", 120, 20);
        product("Pen Pack", "ST-1002", "Stationery", "Sharma Wholesale", "80", "130", 90, 15);
        product("Office Chair", "OE-1002", "Office Equipment", "Metro Suppliers", "5500", "8500", 14, 4);
        expense("Electricity", "3500", ExpenseCategory.UTILITIES);
        expense("Internet", "1800", ExpenseCategory.UTILITIES);
        expense("Office Supplies", "1200", ExpenseCategory.SUPPLIES);
        expense("Transport", "950", ExpenseCategory.OTHER);
        seedDocuments();
    }

    private static int id() { return IDS.incrementAndGet(); }
    private static Timestamp now() { return Timestamp.from(Instant.now()); }
    private static void category(String name, String desc) { Category c=new Category(id(), -1, name, desc); categories.add(c); }
    private static void branch(String name, String code, boolean main) { Branch b=new Branch(); b.setId(id()); b.setBusinessId(-1); b.setName(name); b.setCode(code); b.setMain(main); b.setActive(true); branches.add(b); }
    private static void customer(String name,String email,String phone) { Customer c=new Customer(); c.setId(id());c.setBusinessId(-1);c.setName(name);c.setEmail(email);c.setPhone(phone);c.setActive(true);customers.add(c); }
    private static void supplier(String name) { Supplier s=new Supplier();s.setId(id());s.setBusinessId(-1);s.setName(name);s.setContactPerson(name + " Contact");s.setActive(true);suppliers.add(s); }
    private static void product(String name,String sku,String cat,String sup,String cost,String price,int stock,int min) { Product p=new Product();p.setId(id());p.setBusinessId(-1);p.setName(name);p.setSku(sku);p.setCategoryId(categories.stream().filter(x->cat.equals(x.getName())).findFirst().orElseThrow().getId());p.setCategoryName(cat);p.setSupplierId(suppliers.stream().filter(x->sup.equals(x.getName())).findFirst().orElseThrow().getId());p.setSupplierName(sup);p.setPurchasePrice(new BigDecimal(cost));p.setSellingPrice(new BigDecimal(price));p.setStockQuantity(stock);p.setMinStock(min);products.add(p); }
    private static void expense(String title,String amount,ExpenseCategory cat) { Expense e=new Expense();e.setId(id());e.setBusinessId(-1);e.setTitle(title);e.setAmount(new BigDecimal(amount));e.setCategory(cat);e.setExpenseDate(new Date(System.currentTimeMillis()));e.setBranchId(mainBranch().getId());expenses.add(e); }
    private static void seedDocuments() {
        Sale s=new Sale();s.setBusinessId(-1);s.setBranchId(mainBranch().getId());s.setUserId(-1);s.setCustomerId(customers.get(0).getId());s.setInvoiceNumber("INV-DEMO-1001");
        SaleItem item=new SaleItem();item.setProductId(products.get(1).getId());item.setProductName(products.get(1).getName());item.setQuantity(2);item.setUnitPrice(products.get(1).getSellingPrice());item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(2)));s.getItems().add(item);prepareSale(s, s.getTotal()); createSale(s);
        Purchase p=new Purchase();p.setBusinessId(-1);p.setBranchId(mainBranch().getId());p.setSupplierId(suppliers.get(0).getId());p.setUserId(-1);p.setPurchaseNumber("PO-DEMO-1001");
        PurchaseItem pi=new PurchaseItem();pi.setProductId(products.get(5).getId());pi.setProductName(products.get(5).getName());pi.setQuantity(10);pi.setUnitPrice(products.get(5).getPurchasePrice());pi.setLineTotal(pi.getUnitPrice().multiply(BigDecimal.TEN));p.getItems().add(pi);preparePurchase(p, p.getTotal()); createPurchase(p);
    }

    public static synchronized List<Category> categories(int businessId) { return new ArrayList<>(categories); }
    public static synchronized Category category(int id) { return categories.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public static synchronized int save(Category value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);categories.add(value);} return value.getId(); }
    public static synchronized boolean deleteCategory(int id) { if(products.stream().anyMatch(p->Objects.equals(p.getCategoryId(),id))) return false; return categories.removeIf(x->x.getId()==id); }
    public static synchronized boolean categoryInUse(int id) { return products.stream().anyMatch(p->Objects.equals(p.getCategoryId(),id)); }

    public static synchronized List<Product> products(int businessId) { return new ArrayList<>(products); }
    public static synchronized Product product(int id) { return products.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public static synchronized int save(Product value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);products.add(value);} enrich(value); return value.getId(); }
    public static synchronized boolean deleteProduct(int id) { return products.removeIf(x->x.getId()==id); }
    public static synchronized List<Product> lowStock() { return products.stream().filter(Product::isLowStock).collect(Collectors.toList()); }
    private static void enrich(Product p) { Category c=category(p.getCategoryId()==null?0:p.getCategoryId()); if(c!=null)p.setCategoryName(c.getName()); Supplier s=supplier(p.getSupplierId()==null?0:p.getSupplierId());if(s!=null)p.setSupplierName(s.getName()); }

    public static synchronized List<Customer> customers(int businessId) { customers.forEach(DemoDataStore::customerBalances); return new ArrayList<>(customers); }
    public static synchronized Customer customer(int id) { Customer c=customers.stream().filter(x->x.getId()==id).findFirst().orElse(null);if(c!=null)customerBalances(c);return c; }
    public static synchronized int save(Customer value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);customers.add(value);} return value.getId(); }
    public static synchronized boolean deleteCustomer(int id) { return customers.removeIf(x->x.getId()==id); }
    private static void customerBalances(Customer c){ BigDecimal total=sales.stream().filter(s->Objects.equals(s.getCustomerId(),c.getId())).map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal paid=sales.stream().filter(s->Objects.equals(s.getCustomerId(),c.getId())).map(Sale::getPaidAmount).reduce(BigDecimal.ZERO,BigDecimal::add);c.setTotalPurchased(total);c.setTotalPaid(paid);c.setOutstandingBalance(total.subtract(paid)); }

    public static synchronized List<Supplier> suppliers(int businessId) { suppliers.forEach(DemoDataStore::supplierBalances); return new ArrayList<>(suppliers); }
    public static synchronized Supplier supplier(int id) { Supplier s=suppliers.stream().filter(x->x.getId()==id).findFirst().orElse(null);if(s!=null)supplierBalances(s);return s; }
    public static synchronized int save(Supplier value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);suppliers.add(value);} return value.getId(); }
    public static synchronized boolean deleteSupplier(int id) { return suppliers.removeIf(x->x.getId()==id); }
    private static void supplierBalances(Supplier s){ BigDecimal total=purchases.stream().filter(p->p.getSupplierId()==s.getId()).map(Purchase::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal paid=purchases.stream().filter(p->p.getSupplierId()==s.getId()).map(Purchase::getPaidAmount).reduce(BigDecimal.ZERO,BigDecimal::add);s.setTotalPurchased(total);s.setTotalPaid(paid);s.setOutstandingBalance(total.subtract(paid)); }

    public static synchronized List<Branch> branches(int businessId) { return new ArrayList<>(branches); }
    public static synchronized Branch branch(int id) { return branches.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public static synchronized Branch mainBranch() { return branches.stream().filter(Branch::isMain).findFirst().orElse(null); }
    public static synchronized int save(Branch value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);value.setActive(true);branches.add(value);}return value.getId(); }
    public static synchronized boolean deleteBranch(int id) { return branches.size()>1 && branches.removeIf(x->x.getId()==id); }

    public static synchronized List<Expense> expenses(int businessId) { return new ArrayList<>(expenses); }
    public static synchronized BigDecimal totalExpenses() { return expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add); }
    public static synchronized int save(Expense value) { if(value.getId()==0){value.setId(id());value.setBusinessId(-1);expenses.add(value);}return value.getId(); }
    public static synchronized boolean deleteExpense(int id) { return expenses.removeIf(x->x.getId()==id); }

    public static synchronized List<Sale> sales(int businessId) { return new ArrayList<>(sales); }
    public static synchronized Sale sale(int id) { return sales.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public static synchronized BigDecimal totalSales() { return sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add); }
    public static synchronized boolean createSale(Sale sale) {
        if(sale.getItems()==null||sale.getItems().isEmpty()) return false;
        for(SaleItem i:sale.getItems()){Product p=product(i.getProductId());if(p==null||p.getStockQuantity()<i.getQuantity()) throw new IllegalArgumentException("Insufficient stock for " + (p==null?"selected product":p.getName()));}
        prepareSale(sale, sale.getPaidAmount()); sale.setId(id()); sale.setSaleDate(now()); sale.setCustomerName(sale.getCustomerId()==null?"Walk-in Customer":Optional.ofNullable(customer(sale.getCustomerId())).map(Customer::getName).orElse("Customer")); sale.setBranchName(Optional.ofNullable(branch(sale.getBranchId()==null?0:sale.getBranchId())).map(Branch::getName).orElse("Main Branch"));
        for(SaleItem i:sale.getItems()){i.setSaleId(sale.getId());Product p=product(i.getProductId());p.setStockQuantity(p.getStockQuantity()-i.getQuantity());}
        sales.add(0,sale); if(sale.getCustomerId()!=null&&sale.getPaidAmount().signum()>0) payments.add(new PaymentEntry(id(),"CUSTOMER",sale.getCustomerId(),"SALE",sale.getId(),sale.getPaidAmount(),sale.getPaymentMethod(),"Payment received for invoice " + sale.getInvoiceNumber(),now())); return true;
    }
    public static synchronized void prepareSale(Sale sale, BigDecimal paid) { BigDecimal subtotal=sale.getItems().stream().map(i->{if(i.getLineTotal()==null)i.setLineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));return i.getLineTotal();}).reduce(BigDecimal.ZERO,BigDecimal::add);sale.setSubtotal(subtotal);if(sale.getDiscount()==null)sale.setDiscount(BigDecimal.ZERO);if(sale.getTax()==null)sale.setTax(BigDecimal.ZERO);sale.setTotal(subtotal.subtract(sale.getDiscount()).add(sale.getTax()));sale.setPaidAmount(paid==null?BigDecimal.ZERO:paid);sale.setChangeAmount(sale.getPaidAmount().subtract(sale.getTotal()).max(BigDecimal.ZERO));sale.setPaymentStatus(sale.getPaidAmount().compareTo(sale.getTotal())>=0?PaymentStatus.PAID:(sale.getPaidAmount().signum()>0?PaymentStatus.PARTIAL:PaymentStatus.UNPAID)); }

    public static synchronized List<Purchase> purchases(int businessId) { return new ArrayList<>(purchases); }
    public static synchronized Purchase purchase(int id) { return purchases.stream().filter(x->x.getId()==id).findFirst().orElse(null); }
    public static synchronized boolean createPurchase(Purchase purchase) { if(purchase.getItems()==null||purchase.getItems().isEmpty())return false;preparePurchase(purchase,purchase.getPaidAmount());purchase.setId(id());purchase.setPurchaseDate(now());purchase.setSupplierName(Optional.ofNullable(supplier(purchase.getSupplierId())).map(Supplier::getName).orElse("Supplier"));purchase.setBranchName(Optional.ofNullable(branch(purchase.getBranchId()==null?0:purchase.getBranchId())).map(Branch::getName).orElse("Main Branch"));for(PurchaseItem i:purchase.getItems()){i.setPurchaseId(purchase.getId());Product p=product(i.getProductId());if(p==null)throw new IllegalArgumentException("Product not found");p.setStockQuantity(p.getStockQuantity()+i.getQuantity());}purchases.add(0,purchase);if(purchase.getPaidAmount().signum()>0)payments.add(new PaymentEntry(id(),"SUPPLIER",purchase.getSupplierId(),"PURCHASE",purchase.getId(),purchase.getPaidAmount(),purchase.getPaymentMethod(),"Payment made for purchase " + purchase.getPurchaseNumber(),now()));return true; }
    public static synchronized void preparePurchase(Purchase purchase,BigDecimal paid){BigDecimal subtotal=purchase.getItems().stream().map(i->{if(i.getLineTotal()==null)i.setLineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));return i.getLineTotal();}).reduce(BigDecimal.ZERO,BigDecimal::add);purchase.setSubtotal(subtotal);if(purchase.getDiscount()==null)purchase.setDiscount(BigDecimal.ZERO);if(purchase.getTax()==null)purchase.setTax(BigDecimal.ZERO);purchase.setTotal(subtotal.subtract(purchase.getDiscount()).add(purchase.getTax()));purchase.setPaidAmount(paid==null?BigDecimal.ZERO:paid);purchase.setPaymentStatus(purchase.getPaidAmount().compareTo(purchase.getTotal())>=0?PaymentStatus.PAID:(purchase.getPaidAmount().signum()>0?PaymentStatus.PARTIAL:PaymentStatus.UNPAID));}
    public static synchronized void settleSale(int id,BigDecimal amount,PaymentMethod method,String notes)throws SQLException { Sale s=sale(id);if(s==null)throw new SQLException("The selected document was not found.");settle(s,amount,method,notes);if(s.getCustomerId()!=null)payments.add(new PaymentEntry(IDS.incrementAndGet(),"CUSTOMER",s.getCustomerId(),"SALE",id,amount,method,notes,now())); }
    public static synchronized void settlePurchase(int id,BigDecimal amount,PaymentMethod method,String notes)throws SQLException { Purchase p=purchase(id);if(p==null)throw new SQLException("The selected document was not found.");BigDecimal outstanding=p.getTotal().subtract(p.getPaidAmount());if(amount==null||amount.signum()<=0||amount.compareTo(outstanding)>0)throw new SQLException("Payment must be positive and no more than the outstanding balance.");p.setPaidAmount(p.getPaidAmount().add(amount));p.setPaymentStatus(p.getPaidAmount().compareTo(p.getTotal())==0?PaymentStatus.PAID:PaymentStatus.PARTIAL);payments.add(new PaymentEntry(IDS.incrementAndGet(),"SUPPLIER",p.getSupplierId(),"PURCHASE",id,amount,method,notes,now())); }
    private static void settle(Sale s,BigDecimal amount,PaymentMethod method,String notes)throws SQLException {BigDecimal outstanding=s.getTotal().subtract(s.getPaidAmount());if(amount==null||amount.signum()<=0||amount.compareTo(outstanding)>0)throw new SQLException("Payment must be positive and no more than the outstanding balance.");s.setPaidAmount(s.getPaidAmount().add(amount));s.setPaymentStatus(s.getPaidAmount().compareTo(s.getTotal())==0?PaymentStatus.PAID:PaymentStatus.PARTIAL);}
    public static synchronized List<PaymentEntry> payments() { return new ArrayList<>(payments); }
    public static synchronized List<Object[]> reportRows(ReportFilterDTO filter) {
        String type = filter.getReportType() == null ? "Sales Report" : filter.getReportType();
        List<Object[]> rows = new ArrayList<>();
        if ("Sales Report".equals(type)) for (Sale s : sales) if (matchesSale(s, filter)) rows.add(new Object[]{s.getInvoiceNumber(), s.getCustomerName(), s.getBranchName(), s.getPaymentMethod().getDisplayName(), s.getPaymentStatus().getDisplayName(), "₹" + s.getTotal(), String.valueOf(s.getSaleDate()).substring(0, 16)});
        else if ("Purchase Report".equals(type)) for (Purchase purchase : purchases) if (matchesPurchase(purchase, filter)) rows.add(new Object[]{purchase.getPurchaseNumber(),purchase.getSupplierName(),purchase.getBranchName(),purchase.getPaymentMethod().getDisplayName(),purchase.getPaymentStatus().getDisplayName(),"₹"+purchase.getTotal(),String.valueOf(purchase.getPurchaseDate()).substring(0,16)});
        else if ("Inventory Valuation Report".equals(type)) for (Product p : products) if ((filter.getCategoryId()==null || Objects.equals(filter.getCategoryId(),p.getCategoryId())) && (filter.getSupplierId()==null || Objects.equals(filter.getSupplierId(),p.getSupplierId()))) rows.add(new Object[]{p.getSku(),p.getName(),p.getCategoryName(),filter.getBranchId()==null?"All Branches":Optional.ofNullable(branch(filter.getBranchId())).map(Branch::getName).orElse("Branch"),p.getStockQuantity(),"₹"+p.getPurchasePrice(),"₹"+p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getStockQuantity()))});
        else if ("Expense Breakdown Report".equals(type)) for (Expense e : expenses) if (filter.getBranchId()==null || Objects.equals(filter.getBranchId(),e.getBranchId())) rows.add(new Object[]{"EXP-"+e.getId(),e.getTitle(),e.getCategory().getDisplayName(),Optional.ofNullable(branch(e.getBranchId()==null?0:e.getBranchId())).map(Branch::getName).orElse("Main Branch"),"₹"+e.getAmount(),e.getExpenseDate()});
        else { BigDecimal inventory=products.stream().map(product -> product.getPurchasePrice().multiply(BigDecimal.valueOf(product.getStockQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add); rows.add(new Object[]{"Sales Revenue","₹"+totalSales(),"Completed sales"}); rows.add(new Object[]{"Operating Expenses","₹"+totalExpenses(),"Recorded expenses"}); rows.add(new Object[]{"Inventory Value","₹"+inventory,"Current stock cost"}); }
        return rows;
    }
    private static boolean matchesSale(Sale s, ReportFilterDTO f) { return (f.getCustomerId()==null||Objects.equals(f.getCustomerId(),s.getCustomerId())) && (f.getBranchId()==null||Objects.equals(f.getBranchId(),s.getBranchId())) && (f.getPaymentStatus()==null||f.getPaymentStatus()==s.getPaymentStatus()) && (f.getCategoryId()==null||s.getItems().stream().anyMatch(i->{Product itemProduct=product(i.getProductId());return itemProduct!=null&&Objects.equals(itemProduct.getCategoryId(),f.getCategoryId());})); }
    private static boolean matchesPurchase(Purchase p, ReportFilterDTO f) { return (f.getSupplierId()==null||f.getSupplierId()==p.getSupplierId()) && (f.getBranchId()==null||Objects.equals(f.getBranchId(),p.getBranchId())) && (f.getPaymentStatus()==null||f.getPaymentStatus()==p.getPaymentStatus()); }
}
