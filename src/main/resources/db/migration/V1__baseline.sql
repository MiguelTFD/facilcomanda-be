-- V1__baseline.sql — Feature 025 (adopción de Flyway, coordinación con feature 008).
-- Esquema base equivalente al productivo actual (features 001–024), generado por
-- Hibernate a partir de las entidades JPA vigentes y volcado con pg_dump (schema-only).
-- En la BD productiva existente NO se ejecuta: Flyway hace baseline en la versión 1
-- (baseline-on-migrate) y solo aplica V2 en adelante. Se ejecuta íntegro solo en BD nuevas.
-- Multi-tenancy: la columna organization_id + índices por organización se preservan.


CREATE TABLE public.categories (
    id bigint NOT NULL,
    organization_id bigint,
    parent_category_id bigint,
    description character varying(255),
    name character varying(255)
);

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;

CREATE TABLE public.expenses (
    amount numeric(12,2) NOT NULL,
    expense_date date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    organization_id bigint NOT NULL,
    registered_by_user_id bigint,
    description character varying(255) NOT NULL,
    registered_by_email character varying(255)
);

CREATE SEQUENCE public.expenses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.expenses_id_seq OWNED BY public.expenses.id;

CREATE TABLE public.invoice_payments (
    amount numeric(12,2) NOT NULL,
    id bigint NOT NULL,
    invoice_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    method character varying(20) NOT NULL,
    reference character varying(255),
    CONSTRAINT invoice_payments_method_check CHECK (((method)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'YAPE'::character varying, 'CREDITO'::character varying])::text[])))
);

CREATE SEQUENCE public.invoice_payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.invoice_payments_id_seq OWNED BY public.invoice_payments.id;

CREATE TABLE public.invoices (
    amount_paid numeric(12,2) NOT NULL,
    change_amount numeric(12,2) NOT NULL,
    order_total numeric(12,2) NOT NULL,
    cashier_user_id bigint,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    order_number bigint NOT NULL,
    organization_id bigint NOT NULL,
    paid_at timestamp(6) without time zone NOT NULL,
    restaurant_table_id bigint,
    payment_method character varying(50) NOT NULL,
    invoice_number character varying(80) NOT NULL,
    cashier_email character varying(255),
    notes character varying(255),
    restaurant_table_name character varying(255)
);

CREATE SEQUENCE public.invoices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.invoices_id_seq OWNED BY public.invoices.id;

CREATE TABLE public.order_items (
    quantity integer,
    subtotal numeric(38,2),
    id bigint NOT NULL,
    order_id bigint,
    organization_id bigint,
    product_id bigint,
    comments character varying(255)
);

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;

CREATE TABLE public.orders (
    total numeric(38,2),
    id bigint NOT NULL,
    order_date timestamp(6) without time zone,
    organization_id bigint,
    restaurant_table_id bigint,
    user_id bigint,
    version bigint,
    comments character varying(255),
    idempotency_key character varying(255),
    status character varying(255),
    type character varying(255),
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PREPARING'::character varying, 'READY'::character varying, 'DELIVERED'::character varying, 'PAID'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;

CREATE TABLE public.organizations (
    id bigint NOT NULL,
    name character varying(255),
    tax_identification_number character varying(255),
    tax_identification_type character varying(255)
);

CREATE SEQUENCE public.organizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.organizations_id_seq OWNED BY public.organizations.id;

CREATE TABLE public.product_category (
    category_id bigint NOT NULL,
    product_id bigint NOT NULL
);

CREATE TABLE public.products (
    discount numeric(38,2),
    stock integer,
    unit_price numeric(38,2),
    expiration_date timestamp(6) without time zone,
    id bigint NOT NULL,
    organization_id bigint,
    version bigint,
    description character varying(255),
    name character varying(255)
);

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;

CREATE TABLE public.restaurant_floors (
    id bigint NOT NULL,
    organization_id bigint,
    description character varying(255),
    name character varying(255)
);

CREATE SEQUENCE public.restaurant_floors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.restaurant_floors_id_seq OWNED BY public.restaurant_floors.id;

CREATE TABLE public.restaurant_tables (
    chairs integer,
    floor_id bigint,
    id bigint NOT NULL,
    organization_id bigint,
    description character varying(255),
    name character varying(255),
    state character varying(255),
    CONSTRAINT restaurant_tables_state_check CHECK (((state)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'OCCUPIED'::character varying])::text[])))
);

CREATE SEQUENCE public.restaurant_tables_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.restaurant_tables_id_seq OWNED BY public.restaurant_tables.id;

CREATE TABLE public.roles (
    id bigint NOT NULL,
    organization_id bigint,
    description character varying(255),
    name character varying(255),
    CONSTRAINT roles_name_check CHECK (((name)::text = ANY ((ARRAY['MESERO'::character varying, 'CAJERO'::character varying, 'COCINERO'::character varying, 'ADMIN'::character varying, 'SUPERADMIN'::character varying])::text[])))
);

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;

CREATE TABLE public.users (
    id bigint NOT NULL,
    organization_id bigint,
    role_id bigint,
    email character varying(255),
    first_name character varying(255),
    identity_number character varying(255),
    identity_type character varying(255),
    last_name character varying(255),
    password character varying(255),
    phone character varying(255)
);

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);

ALTER TABLE ONLY public.expenses ALTER COLUMN id SET DEFAULT nextval('public.expenses_id_seq'::regclass);

ALTER TABLE ONLY public.invoice_payments ALTER COLUMN id SET DEFAULT nextval('public.invoice_payments_id_seq'::regclass);

ALTER TABLE ONLY public.invoices ALTER COLUMN id SET DEFAULT nextval('public.invoices_id_seq'::regclass);

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);

ALTER TABLE ONLY public.organizations ALTER COLUMN id SET DEFAULT nextval('public.organizations_id_seq'::regclass);

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);

ALTER TABLE ONLY public.restaurant_floors ALTER COLUMN id SET DEFAULT nextval('public.restaurant_floors_id_seq'::regclass);

ALTER TABLE ONLY public.restaurant_tables ALTER COLUMN id SET DEFAULT nextval('public.restaurant_tables_id_seq'::regclass);

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT invoice_payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_invoice_number_key UNIQUE (invoice_number);

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_order_id_key UNIQUE (order_id);

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_organization_id_idempotency_key_key UNIQUE (organization_id, idempotency_key);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT product_category_pkey PRIMARY KEY (category_id, product_id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.restaurant_floors
    ADD CONSTRAINT restaurant_floors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.restaurant_tables
    ADD CONSTRAINT restaurant_tables_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_category_org ON public.categories USING btree (organization_id);

CREATE INDEX idx_expense_org ON public.expenses USING btree (organization_id);

CREATE INDEX idx_expense_org_date ON public.expenses USING btree (organization_id, expense_date);

CREATE INDEX idx_floor_org ON public.restaurant_floors USING btree (organization_id);

CREATE INDEX idx_invoice_order ON public.invoices USING btree (order_id);

CREATE INDEX idx_invoice_org ON public.invoices USING btree (organization_id);

CREATE INDEX idx_invoice_payment_invoice ON public.invoice_payments USING btree (invoice_id);

CREATE INDEX idx_invoice_payment_org ON public.invoice_payments USING btree (organization_id);

CREATE INDEX idx_order_item_org ON public.order_items USING btree (organization_id);

CREATE INDEX idx_order_org ON public.orders USING btree (organization_id);

CREATE INDEX idx_product_org ON public.products USING btree (organization_id);

CREATE INDEX idx_role_org ON public.roles USING btree (organization_id);

CREATE INDEX idx_table_org ON public.restaurant_tables USING btree (organization_id);

CREATE INDEX idx_user_org ON public.users USING btree (organization_id);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk32ql8ubntj5uh44ph9659tiih FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk4ko3y00tkkk2ya3p6wnefjj2f FOREIGN KEY (order_id) REFERENCES public.orders(id);

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fk5w81wp3eyugvi2lii94iao3fm FOREIGN KEY (product_id) REFERENCES public.products(id);

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk9il7y6fehxwunjeepq0n7g5rd FOREIGN KEY (parent_category_id) REFERENCES public.categories(id);

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT fkaa9if3io1iupfuqgsm0fbuch9 FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(id);

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fkcjwe1jff2gfo9l7ldx37xxvtm FOREIGN KEY (cashier_user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fkdswxvx2nl2032yjv609r29sdr FOREIGN KEY (category_id) REFERENCES public.categories(id);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fkgowqti7ase2e3o3k76kuani2w FOREIGN KEY (restaurant_table_id) REFERENCES public.restaurant_tables(id);

ALTER TABLE ONLY public.restaurant_tables
    ADD CONSTRAINT fkny07ea4u5p98kb3djeeipba3r FOREIGN KEY (floor_id) REFERENCES public.restaurant_floors(id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkocimc7dtr037rh4ls4l95nlfi FOREIGN KEY (product_id) REFERENCES public.products(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkp56c1712k691lhsyewcssf40f FOREIGN KEY (role_id) REFERENCES public.roles(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkqpugllwvyv37klq7ft9m8aqxk FOREIGN KEY (organization_id) REFERENCES public.organizations(id);

