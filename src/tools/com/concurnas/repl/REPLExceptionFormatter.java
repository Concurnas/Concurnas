package com.concurnas.repl;

public class REPLExceptionFormatter {
	public static String formatException(String classBeingRun, String executorName, Throwable thrown) {
		String classBeingRunGlob = classBeingRun + "$Globals$";
		StringBuilder sb = new StringBuilder("|  "  + thrown.toString()+"\n");
		StackTraceElement[] elms = thrown.getStackTrace();
		int sz = elms.length;
		for(int n=0; n < sz; n++) {
			StackTraceElement elm = elms[n];
			String methodName = elm.getMethodName();
			String clsName = elm.getClassName();
			if(clsName.equals(executorName) && methodName.equals("apply")) {
				break;//cut off below apply
			}
			else if(methodName.equals("getInstance?")) {
				continue;
			}
			else if(clsName.equals(classBeingRunGlob)) {
				if(methodName.equals("init")) {
					sb.append(String.format("|    at line:%s", elm.getLineNumber()));
				}else {
					sb.append(String.format("|    at %s(line:%s)", methodName, elm.getLineNumber()));
				}
			}
			else if(methodName.equals("init")) {
				continue;
			}else {
				sb.append("|    " + elm.toString());
			}
			sb.append("\n");
		}
		sb.deleteCharAt(sb.length()-1);
		
		return sb.toString();
	}
}