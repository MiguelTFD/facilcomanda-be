package com.facilcomanda.erp.seed;

import com.facilcomanda.erp.model.Category;
import com.facilcomanda.erp.model.Invoice;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import com.facilcomanda.erp.model.Organization;
import com.facilcomanda.erp.model.Product;
import com.facilcomanda.erp.model.RestaurantFloor;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.model.Role;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.model.enums.TableState;
import com.facilcomanda.erp.repository.CategoryRepository;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.OrganizationRepository;
import com.facilcomanda.erp.repository.ProductRepository;
import com.facilcomanda.erp.repository.RestaurantFloorRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.RoleRepository;
import com.facilcomanda.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra un conjunto de datos de demostración para una organización:
 * organización, roles, usuarios, pisos, mesas, categorías (con jerarquía),
 * productos (con sus categorías), órdenes con ítems y facturas.
 *
 * <p>Solo se ejecuta bajo el perfil de Spring {@code seed}. Es idempotente
 * por tabla: cada bloque comprueba si ya hay datos de esa entidad para la
 * organización y, si los hay, los reutiliza en lugar de duplicarlos. Nunca
 * ejecuta operaciones destructivas (DROP/DELETE/TRUNCATE).</p>
 *
 * <p>Ejecutar con: {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed}
 * o {@code SPRING_PROFILES_ACTIVE=seed}.</p>
 */
@Component
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Contraseña en claro para todos los usuarios semilla (se almacena con BCrypt). */
    private static final String SEED_PASSWORD = "password123";

    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RestaurantFloorRepository floorRepository;
    private final RestaurantTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(OrganizationRepository organizationRepository,
                      RoleRepository roleRepository,
                      UserRepository userRepository,
                      RestaurantFloorRepository floorRepository,
                      RestaurantTableRepository tableRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      OrderRepository orderRepository,
                      InvoiceRepository invoiceRepository,
                      PasswordEncoder passwordEncoder,
                      Clock clock) {
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.floorRepository = floorRepository;
        this.tableRepository = tableRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Organization organization = resolveOrganization();
        Long orgId = organization.getId();

        Map<RoleName, Role> roles = resolveRoles(organization);
        seedUsers(organization, roles);

        List<RestaurantFloor> floors = seedFloors(organization);
        Map<String, RestaurantTable> tables = seedTables(organization, floors);
        Map<String, Category> categories = seedCategories(organization);
        Map<String, Product> products = seedProducts(organization, categories);
        seedOrdersAndInvoices(organization, tables, products);

        log.info("[seed] Datos de demostración listos para organización id={} (contraseña común: '{}').",
                orgId, SEED_PASSWORD);
    }

    // --- Organización -------------------------------------------------------

    private Organization resolveOrganization() {
        List<Organization> existing = organizationRepository.findAll();
        if (!existing.isEmpty()) {
            log.info("[seed] Organización existente reutilizada id={}.", existing.get(0).getId());
            return existing.get(0);
        }
        Organization organization = organizationRepository.save(
                new Organization("Restaurante Demo FacilComanda", "RUC", "20123456789"));
        log.info("[seed] Organización creada id={}.", organization.getId());
        return organization;
    }

    // --- Roles --------------------------------------------------------------

    private Map<RoleName, Role> resolveRoles(Organization organization) {
        Map<RoleName, Role> byName = new EnumMap<>(RoleName.class);
        for (Role role : roleRepository.findByOrganizationId(organization.getId())) {
            byName.put(role.getName(), role);
        }
        for (RoleName roleName : RoleName.values()) {
            byName.computeIfAbsent(roleName, name -> roleRepository.save(
                    new Role(organization.getId(), name, "Rol " + name.name())));
        }
        log.info("[seed] Roles disponibles: {}.", byName.size());
        return byName;
    }

    // --- Usuarios -----------------------------------------------------------

    private void seedUsers(Organization organization, Map<RoleName, Role> roles) {
        if (!userRepository.findByOrganizationId(organization.getId()).isEmpty()) {
            log.info("[seed] Usuarios ya existentes; no se crean nuevos.");
            return;
        }
        createUser(organization, roles.get(RoleName.ADMIN),
                "admin@facilcomanda.local", "Admin", "Principal", "12345678");
        createUser(organization, roles.get(RoleName.SUPERADMIN),
                "superadmin@facilcomanda.local", "Super", "Admin", "12345679");
        createUser(organization, roles.get(RoleName.MESERO),
                "mesero@facilcomanda.local", "Mesero", "Uno", "12345680");
        createUser(organization, roles.get(RoleName.CAJERO),
                "cajero@facilcomanda.local", "Cajero", "Uno", "12345681");
        createUser(organization, roles.get(RoleName.COCINERO),
                "cocinero@facilcomanda.local", "Cocinero", "Uno", "12345682");
        log.info("[seed] 5 usuarios creados (uno por rol).");
    }

    private void createUser(Organization organization, Role role, String email,
                            String firstName, String lastName, String identityNumber) {
        User user = new User(
                organization.getId(),
                role,
                email,
                passwordEncoder.encode(SEED_PASSWORD),
                firstName,
                lastName,
                "DNI",
                identityNumber,
                "600000000");
        user.setOrganization(organization);
        userRepository.save(user);
    }

    // --- Pisos --------------------------------------------------------------

    private List<RestaurantFloor> seedFloors(Organization organization) {
        List<RestaurantFloor> existing = floorRepository.findByOrganizationId(organization.getId());
        if (!existing.isEmpty()) {
            log.info("[seed] Pisos ya existentes; se reutilizan {}.", existing.size());
            return existing;
        }
        List<RestaurantFloor> floors = new ArrayList<>();
        floors.add(floorRepository.save(
                new RestaurantFloor(organization.getId(), "Salón Principal", "Salón interior del restaurante")));
        floors.add(floorRepository.save(
                new RestaurantFloor(organization.getId(), "Terraza", "Zona al aire libre")));
        log.info("[seed] {} pisos creados.", floors.size());
        return floors;
    }

    // --- Mesas --------------------------------------------------------------

    private Map<String, RestaurantTable> seedTables(Organization organization, List<RestaurantFloor> floors) {
        Map<String, RestaurantTable> byName = new LinkedHashMap<>();
        List<RestaurantTable> existing = tableRepository.findByOrganizationId(organization.getId());
        if (!existing.isEmpty()) {
            existing.forEach(t -> byName.put(t.getName(), t));
            log.info("[seed] Mesas ya existentes; se reutilizan {}.", existing.size());
            return byName;
        }
        RestaurantFloor salon = floors.get(0);
        RestaurantFloor terraza = floors.size() > 1 ? floors.get(1) : floors.get(0);

        byName.put("Mesa 1", createTable(organization, salon, "Mesa 1", TableState.AVAILABLE, 4));
        byName.put("Mesa 2", createTable(organization, salon, "Mesa 2", TableState.AVAILABLE, 2));
        byName.put("Mesa 3", createTable(organization, salon, "Mesa 3", TableState.OCCUPIED, 6));
        byName.put("Mesa 4", createTable(organization, terraza, "Mesa 4", TableState.AVAILABLE, 4));
        byName.put("Mesa 5", createTable(organization, terraza, "Mesa 5", TableState.OCCUPIED, 2));
        byName.put("Barra 1", createTable(organization, terraza, "Barra 1", TableState.AVAILABLE, 3));
        log.info("[seed] {} mesas creadas.", byName.size());
        return byName;
    }

    private RestaurantTable createTable(Organization organization, RestaurantFloor floor, String name,
                                        TableState state, int chairs) {
        RestaurantTable table = new RestaurantTable(organization.getId(), name,
                "Mesa " + name + " del piso " + floor.getName(), state, chairs);
        table.setFloor(floor);
        return tableRepository.save(table);
    }

    // --- Categorías (con jerarquía) ----------------------------------------

    private Map<String, Category> seedCategories(Organization organization) {
        Map<String, Category> byName = new LinkedHashMap<>();
        List<Category> existing = categoryRepository.findByOrganizationId(organization.getId());
        if (!existing.isEmpty()) {
            existing.forEach(c -> byName.put(c.getName(), c));
            log.info("[seed] Categorías ya existentes; se reutilizan {}.", existing.size());
            return byName;
        }
        Category bebidas = createCategory(organization, "Bebidas", "Todas las bebidas", null);
        Category comidas = createCategory(organization, "Comidas", "Todos los platos", null);
        byName.put("Bebidas", bebidas);
        byName.put("Comidas", comidas);
        byName.put("Gaseosas", createCategory(organization, "Gaseosas", "Bebidas gaseosas", bebidas));
        byName.put("Cervezas", createCategory(organization, "Cervezas", "Cervezas nacionales", bebidas));
        byName.put("Entradas", createCategory(organization, "Entradas", "Entradas y piqueos", comidas));
        byName.put("Platos de fondo", createCategory(organization, "Platos de fondo", "Platos principales", comidas));
        byName.put("Postres", createCategory(organization, "Postres", "Postres tradicionales", comidas));
        log.info("[seed] {} categorías creadas (2 padre + 5 hijas).", byName.size());
        return byName;
    }

    private Category createCategory(Organization organization, String name, String description, Category parent) {
        return categoryRepository.save(new Category(organization.getId(), name, description, parent));
    }

    // --- Productos (con categorías) ----------------------------------------

    private Map<String, Product> seedProducts(Organization organization, Map<String, Category> categories) {
        Map<String, Product> byName = new LinkedHashMap<>();
        List<Product> existing = productRepository.findByOrganizationId(organization.getId());
        if (!existing.isEmpty()) {
            existing.forEach(p -> byName.put(p.getName(), p));
            log.info("[seed] Productos ya existentes; se reutilizan {}.", existing.size());
            return byName;
        }
        Category bebidas = categories.get("Bebidas");
        Category comidas = categories.get("Comidas");
        Category gaseosas = categories.get("Gaseosas");
        Category cervezas = categories.get("Cervezas");
        Category entradas = categories.get("Entradas");
        Category fondos = categories.get("Platos de fondo");
        Category postres = categories.get("Postres");

        byName.put("Inca Kola 500ml", createProduct(organization, "Inca Kola 500ml",
                "Gaseosa Inca Kola personal", 100, "6.00", Set.of(bebidas, gaseosas)));
        byName.put("Coca Cola 500ml", createProduct(organization, "Coca Cola 500ml",
                "Gaseosa Coca Cola personal", 100, "6.00", Set.of(bebidas, gaseosas)));
        byName.put("Cerveza Pilsen", createProduct(organization, "Cerveza Pilsen",
                "Cerveza Pilsen 305ml", 80, "10.00", Set.of(bebidas, cervezas)));
        byName.put("Ceviche", createProduct(organization, "Ceviche",
                "Ceviche de pescado fresco", 40, "32.00", Set.of(comidas, entradas)));
        byName.put("Lomo Saltado", createProduct(organization, "Lomo Saltado",
                "Lomo saltado con papas y arroz", 50, "30.00", Set.of(comidas, fondos)));
        byName.put("Ají de Gallina", createProduct(organization, "Ají de Gallina",
                "Ají de gallina con arroz", 45, "25.00", Set.of(comidas, fondos)));
        byName.put("Arroz con Pollo", createProduct(organization, "Arroz con Pollo",
                "Arroz con pollo a la peruana", 45, "24.00", Set.of(comidas, fondos)));
        byName.put("Suspiro a la Limeña", createProduct(organization, "Suspiro a la Limeña",
                "Postre tradicional limeño", 30, "14.00", Set.of(comidas, postres)));
        log.info("[seed] {} productos creados.", byName.size());
        return byName;
    }

    private Product createProduct(Organization organization, String name, String description, int stock,
                                  String unitPrice, Set<Category> productCategories) {
        Product product = new Product(organization.getId(), name, description, stock,
                new BigDecimal(unitPrice), BigDecimal.ZERO, null);
        product.setCategories(productCategories);
        return productRepository.save(product);
    }

    // --- Órdenes, ítems y facturas -----------------------------------------

    private void seedOrdersAndInvoices(Organization organization, Map<String, RestaurantTable> tables,
                                       Map<String, Product> products) {
        if (!orderRepository.findByOrganizationId(organization.getId()).isEmpty()) {
            log.info("[seed] Órdenes ya existentes; no se crean nuevas.");
            return;
        }
        User mesero = userRepository.findByEmail("mesero@facilcomanda.local").orElse(null);
        User cajero = userRepository.findByEmail("cajero@facilcomanda.local").orElse(null);
        if (mesero == null) {
            log.warn("[seed] No se encontró el usuario mesero; se omiten las órdenes.");
            return;
        }

        // Orden 1 — en preparación, mesa ocupada.
        Order order1 = createOrder(organization, tables.get("Mesa 3"), mesero, OrderStatus.PREPARING,
                "seed-order-1", "Sin ají, por favor");
        addItem(organization, order1, products.get("Lomo Saltado"), 2);
        addItem(organization, order1, products.get("Inca Kola 500ml"), 2);
        finalizeOrder(order1);

        // Orden 2 — pendiente, mesa ocupada.
        Order order2 = createOrder(organization, tables.get("Mesa 5"), mesero, OrderStatus.PENDING,
                "seed-order-2", null);
        addItem(organization, order2, products.get("Ceviche"), 1);
        addItem(organization, order2, products.get("Cerveza Pilsen"), 2);
        finalizeOrder(order2);

        // Orden 3 — pagada, con factura.
        Order order3 = createOrder(organization, tables.get("Mesa 1"), mesero, OrderStatus.PAID,
                "seed-order-3", null);
        addItem(organization, order3, products.get("Ají de Gallina"), 1);
        addItem(organization, order3, products.get("Coca Cola 500ml"), 1);
        Order savedOrder3 = finalizeOrder(order3);
        createInvoiceFor(organization, savedOrder3, cajero);

        log.info("[seed] 3 órdenes con ítems creadas (1 pagada con factura).");
    }

    private Order createOrder(Organization organization, RestaurantTable table, User user, OrderStatus status,
                              String idempotencyKey, String comments) {
        Order order = new Order(organization.getId(), LocalDateTime.now(clock), "LOCAL", comments, status,
                table, user, BigDecimal.ZERO);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }

    private void addItem(Organization organization, Order order, Product product, int quantity) {
        BigDecimal subtotal = product.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        OrderItem item = new OrderItem(organization.getId(), order, product, quantity, subtotal);
        order.addOrderItem(item);
    }

    private Order finalizeOrder(Order order) {
        BigDecimal total = order.getOrderItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);
        // save() devuelve la instancia gestionada con id asignado: por el @Version de Order,
        // Spring Data ejecuta merge (no persist), así que hay que usar el retorno, no la original.
        return orderRepository.save(order);
    }

    private void createInvoiceFor(Organization organization, Order order, User cashier) {
        if (invoiceRepository.existsByOrder_IdAndOrganizationId(order.getId(), organization.getId())) {
            return;
        }
        BigDecimal total = order.getTotal().setScale(2);
        BigDecimal amountPaid = total.add(new BigDecimal("19.00"));
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organization.getId());
        invoice.setOrder(order);
        invoice.setOrderNumber(order.getId());
        invoice.setInvoiceNumber(String.format("B001-%06d", order.getId()));
        invoice.setOrderTotal(total);
        invoice.setAmountPaid(amountPaid);
        invoice.setChangeAmount(amountPaid.subtract(total));
        invoice.setPaymentMethod("EFECTIVO");
        invoice.setPaidAt(LocalDateTime.now(clock));
        if (cashier != null) {
            invoice.setCashier(cashier);
            invoice.setCashierEmail(cashier.getEmail());
        }
        if (order.getRestaurantTable() != null) {
            invoice.setRestaurantTableId(order.getRestaurantTable().getId());
            invoice.setRestaurantTableName(order.getRestaurantTable().getName());
        }
        invoice.setNotes("Factura de demostración generada por el seeder");
        invoiceRepository.save(invoice);
    }
}
