package calculator.api;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalculatorServiceTest {
    @Test
    public void testAdd() {
        CalculatorService service = new CalculatorServiceImpl();
        assertEquals(3.0, service.add(1, 2), 0.001);
    }
} 