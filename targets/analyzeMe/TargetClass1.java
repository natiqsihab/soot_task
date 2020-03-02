package analyzeMe;

public class TargetClass1 {

	private final String whatsoever = "Test";

	private void leak(String data) {
		System.out.println("Leak: " + data);
	}

	// Test 1
	// No need

	// Test 2
	public void sourceToSink2() {
		String x = getSecret();
		//x = x + "";
		x = "test";
		leak(x);
	}

	// Test 3
	public void sourceToSink3() {
		String x = getSecret();
		String y = x;
		leak(y);
	}

	// Test 4
	public void sourceToSink4() {
		String x = getSecret();
		//String y = x;
		//x = "test";
		//leak(x);
	}

	// Test 5
	public void sourceToSink5() {
		String x = getSecret();
//		taintedClass taintedClass=new taintedClass();
//		taintedClass.a=x;
//		leak(taintedClass.a);
	}

	// Test 6
	public void sourceToSink6() {
		String x = getSecret();

	}

	private String getSecret() {
		return "top secret";
	}

	
//	 class taintedClass {
//		taintedClass(){
//			
//		}
//		taintedClass(String a){
//			
//		}
//		public String a;
//		public String b;
//	}


}