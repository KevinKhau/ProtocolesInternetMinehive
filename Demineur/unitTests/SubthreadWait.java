package unitTests;

public class SubthreadWait {

	public static void main(String[] args) {
		new SubthreadWait();
	}

	public SubthreadWait() {
		SubClass t = new SubClass();
		t.start();
		synchronized (t) {
			try {
				while (true) {
					System.out.println(t.count);
					t.wait();
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public class SubClass extends Thread {
		int count = 0;
		public SubClass() {

		}

		@Override
		public void run() {
			while (true) {
				synchronized (this) {
					for (int i = 0; i < 5; i++) {
						count++;
						System.out.println("increasing");
						notify();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} 
			}
		}

	}

}
