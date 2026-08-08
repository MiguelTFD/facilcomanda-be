package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ExpenseRequest;
import com.facilcomanda.erp.dto.ExpenseResponse;
import com.facilcomanda.erp.exception.ExpenseNotFoundException;
import com.facilcomanda.erp.exception.ExpenseValidationException;
import com.facilcomanda.erp.model.Expense;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 022 — CA2: CRUD de egresos. Valida monto &gt; 0, descripción
 * obligatoria y fecha requerida con excepciones de dominio (4xx), registra al
 * autor desde el token y hace toda consulta acotada al {@code organizationId}
 * (aislamiento multi-tenant).
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long ORG_ID = 7L;
    private static final Long OTHER_ORG_ID = 99L;
    private static final Long EXPENSE_ID = 55L;
    private static final String AUTHOR_EMAIL = "cajero@facilcomanda.test";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 18);
    /** Feature 033: las 18:30 del 08/08/2026 en Lima son las 23:30 UTC. */
    private static final LocalDateTime LIMA_NOW = LocalDateTime.of(2026, 8, 8, 18, 30);

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private UserRepository userRepository;

    /**
     * Reloj fijo inyectado por constructor. Va como {@code @Spy} y nunca como
     * {@code @Mock}: un mock de {@link Clock} devolvería {@code null} en
     * {@code instant()} y reventaría con NPE en cada test que estampe una fecha.
     */
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-08-08T23:30:00Z"), ZoneId.of("America/Lima"));

    @InjectMocks
    private ExpenseService expenseService;

    @Captor
    private ArgumentCaptor<Expense> expenseCaptor;

    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(3L);
        author.setEmail(AUTHOR_EMAIL);
    }

    private void stubAuthorAndSave() {
        when(userRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(author));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Expense existingExpense() {
        Expense expense = new Expense();
        expense.setId(EXPENSE_ID);
        expense.setOrganizationId(ORG_ID);
        expense.setAmount(new BigDecimal("40.00"));
        expense.setDescription("Gas");
        expense.setExpenseDate(TODAY);
        expense.setRegisteredByUserId(author.getId());
        expense.setRegisteredByEmail(AUTHOR_EMAIL);
        return expense;
    }

    private ExpenseResponse create(ExpenseRequest request) {
        return expenseService.createExpense(request, ORG_ID, AUTHOR_EMAIL);
    }

    // ---------- Alta ----------

    @Test
    void createExpense_datosValidos_guardaConAutorDelTokenYOrganizacion() {
        stubAuthorAndSave();

        ExpenseResponse response = create(new ExpenseRequest(new BigDecimal("40.50"), "Compra de gas", TODAY));

        verify(expenseRepository).save(expenseCaptor.capture());
        Expense saved = expenseCaptor.getValue();
        assertThat(saved.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(saved.getAmount()).isEqualByComparingTo("40.50");
        assertThat(saved.getDescription()).isEqualTo("Compra de gas");
        assertThat(saved.getExpenseDate()).isEqualTo(TODAY);
        assertThat(saved.getRegisteredByUserId()).isEqualTo(author.getId());
        assertThat(saved.getRegisteredByEmail()).isEqualTo(AUTHOR_EMAIL);
        assertThat(saved.getCreatedAt()).isNotNull();

        assertThat(response.amount()).isEqualByComparingTo("40.50");
        assertThat(response.registeredByEmail()).isEqualTo(AUTHOR_EMAIL);
    }

    @Test
    void createExpense_recortaLosEspaciosDeLaDescripcion() {
        stubAuthorAndSave();

        create(new ExpenseRequest(new BigDecimal("10.00"), "  Taxi mercado  ", TODAY));

        verify(expenseRepository).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getDescription()).isEqualTo("Taxi mercado");
    }

    // ---------- Feature 033: createdAt en hora de Lima, expenseDate intacta ----------

    @Test
    void createExpense_sellaCreatedAtConLaHoraDeLimaDelReloj() {
        stubAuthorAndSave();

        create(new ExpenseRequest(new BigDecimal("40.50"), "Compra de gas", TODAY));

        verify(expenseRepository).save(expenseCaptor.capture());
        // 18:30 de Lima, no las 23:30 UTC del mismo instante.
        assertThat(expenseCaptor.getValue().getCreatedAt()).isEqualTo(LIMA_NOW);
    }

    /**
     * Decisión 7 de la spec 033: {@code expenseDate} la elige una persona en un
     * selector, no la estampa el reloj. Este test existe para impedir que el
     * arreglo de zona horaria la desplace también.
     */
    @Test
    void createExpense_noDesplazaLaExpenseDateQueLlegaEnElRequest() {
        stubAuthorAndSave();

        create(new ExpenseRequest(new BigDecimal("40.50"), "Compra de gas", TODAY));

        verify(expenseRepository).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getExpenseDate()).isEqualTo(TODAY);
    }

    // ---------- Validaciones de dominio (4xx) ----------

    @Test
    void createExpense_montoCero_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(BigDecimal.ZERO, "Gas", TODAY)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_montoNegativo_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(new BigDecimal("-1.00"), "Gas", TODAY)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_montoNulo_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(null, "Gas", TODAY)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_descripcionEnBlanco_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(new BigDecimal("10.00"), "   ", TODAY)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_descripcionNula_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(new BigDecimal("10.00"), null, TODAY)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_fechaNula_lanzaExpenseValidationException() {
        assertThatThrownBy(() -> create(new ExpenseRequest(new BigDecimal("10.00"), "Gas", null)))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    // ---------- Listado, edición y borrado ----------

    @Test
    void getExpenses_consultaSoloElRangoDeLaOrganizacion() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(expenseRepository.findByOrganizationIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
                ORG_ID, from, to)).thenReturn(List.of(existingExpense()));

        List<ExpenseResponse> responses = expenseService.getExpenses(ORG_ID, from, to);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(EXPENSE_ID);
        assertThat(responses.get(0).description()).isEqualTo("Gas");
    }

    @Test
    void updateExpense_corrigeMontoDescripcionYFecha() {
        Expense expense = existingExpense();
        when(expenseRepository.findByIdAndOrganizationId(EXPENSE_ID, ORG_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate corrected = TODAY.minusDays(1);
        ExpenseResponse response = expenseService.updateExpense(EXPENSE_ID,
                new ExpenseRequest(new BigDecimal("62.00"), "Gas y carbón", corrected), ORG_ID);

        assertThat(response.amount()).isEqualByComparingTo("62.00");
        assertThat(response.description()).isEqualTo("Gas y carbón");
        assertThat(response.expenseDate()).isEqualTo(corrected);
        // El autor original no se reescribe al corregir.
        assertThat(response.registeredByEmail()).isEqualTo(AUTHOR_EMAIL);
    }

    @Test
    void updateExpense_montoInvalido_lanzaExpenseValidationExceptionYNoGuarda() {
        assertThatThrownBy(() -> expenseService.updateExpense(EXPENSE_ID,
                new ExpenseRequest(BigDecimal.ZERO, "Gas", TODAY), ORG_ID))
                .isInstanceOf(ExpenseValidationException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void deleteExpense_eliminaElEgresoDeLaOrganizacion() {
        Expense expense = existingExpense();
        when(expenseRepository.findByIdAndOrganizationId(EXPENSE_ID, ORG_ID)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(EXPENSE_ID, ORG_ID);

        verify(expenseRepository).delete(expense);
    }

    // ---------- Aislamiento multi-tenant ----------

    @Test
    void updateExpense_egresoDeOtraOrganizacion_lanzaExpenseNotFoundException() {
        when(expenseRepository.findByIdAndOrganizationId(EXPENSE_ID, OTHER_ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.updateExpense(EXPENSE_ID,
                new ExpenseRequest(new BigDecimal("10.00"), "Gas", TODAY), OTHER_ORG_ID))
                .isInstanceOf(ExpenseNotFoundException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void deleteExpense_egresoDeOtraOrganizacion_lanzaExpenseNotFoundExceptionYNoBorra() {
        when(expenseRepository.findByIdAndOrganizationId(EXPENSE_ID, OTHER_ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.deleteExpense(EXPENSE_ID, OTHER_ORG_ID))
                .isInstanceOf(ExpenseNotFoundException.class);
        verify(expenseRepository, never()).delete(any());
    }
}
