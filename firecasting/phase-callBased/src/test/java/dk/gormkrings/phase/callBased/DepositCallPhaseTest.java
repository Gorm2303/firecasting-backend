package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.specification.ISpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DepositCallPhaseTest {

    @Mock
    private ISpecification spec;

    @Mock
    private ILiveData liveData;

    @Mock
    private IReturner returner;

    // We can either mock IDate or use a dummy implementation.
    @Mock
    private IDate date;

    private Deposit deposit;
    private DepositCallPhase depositPhase;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testOnPhaseStart_DepositsInitialAmount() {

    }

    @Test
    void testOnMonthEnd_SubsequentCallsIncludeReturnAndMonthlyDeposit() {

    }

    @Test
    void testCopyCreatesIndependentInstance() {

    }

    @Test
    void testOnMonthEnd_CallsAddReturnAndUpdatesLiveData() {

    }

    @Test
    void testDepositMoney_DirectInvocation() {

    }

    @Test
    void testDepositMoney_WithZeroMonthlyDeposit() {

    }
}
