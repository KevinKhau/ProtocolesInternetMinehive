package unitTests;

import static org.junit.Assert.*;

import org.junit.Test;

public class ThrowExceptionTest {

	@Test
	public void test() {
		
	}

	public void t() throws Exception {
		throw new Exception("Test");
	}
	
	public void t2() throws Exception {
		try {
			t();
		} catch (Exception e) {
			throw e;
//			System.out.println("reachable ?");
		}
	}
}
