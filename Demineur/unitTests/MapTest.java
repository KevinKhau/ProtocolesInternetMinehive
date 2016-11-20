package unitTests;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapTest {

	public static void main(String[] args) {
		Map<Integer, String> map = new ConcurrentHashMap<>();
		System.out.println(Integer.parseInt(null));
		List<? extends Number> l = new LinkedList<>();
		List<Integer> se = new LinkedList<>();
		se.add(3);
		se.add(-1);
		l = se;
		se.add(343);
		l.add(null);
//		l.add(354);
	}

}
