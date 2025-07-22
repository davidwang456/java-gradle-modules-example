package calculator.service;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdvancedCalculatorServiceTest {
    
    private AdvancedCalculatorService advancedCalculatorService;
    
    @Before
    public void setUp() {
        advancedCalculatorService = new AdvancedCalculatorService();
    }
    
    @Test
    public void testAverage() {
        assertEquals(2.5, advancedCalculatorService.average(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals(0.0, advancedCalculatorService.average(), 0.001);
        assertEquals(5.0, advancedCalculatorService.average(5.0), 0.001);
    }
    
    @Test
    public void testSquare() {
        assertEquals(4.0, advancedCalculatorService.square(2.0), 0.001);
        assertEquals(9.0, advancedCalculatorService.square(3.0), 0.001);
        assertEquals(0.0, advancedCalculatorService.square(0.0), 0.001);
    }
    
    @Test
    public void testCube() {
        assertEquals(8.0, advancedCalculatorService.cube(2.0), 0.001);
        assertEquals(27.0, advancedCalculatorService.cube(3.0), 0.001);
        assertEquals(0.0, advancedCalculatorService.cube(0.0), 0.001);
    }
    
    @Test
    public void testFactorial() {
        assertEquals(1.0, advancedCalculatorService.factorial(0), 0.001);
        assertEquals(1.0, advancedCalculatorService.factorial(1), 0.001);
        assertEquals(2.0, advancedCalculatorService.factorial(2), 0.001);
        assertEquals(6.0, advancedCalculatorService.factorial(3), 0.001);
        assertEquals(24.0, advancedCalculatorService.factorial(4), 0.001);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFactorialNegative() {
        advancedCalculatorService.factorial(-1);
    }
} 