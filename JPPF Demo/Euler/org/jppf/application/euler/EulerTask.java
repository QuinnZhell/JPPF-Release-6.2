
package org.jppf.application.euler;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.JPPFCallable;

public class EulerTask extends AbstractTask<String> {
  /**
   * Perform initializations on the client side,
   * before the task is executed by the node.
   */
  public EulerTask() {
    // perform initializations here ...
  }

  /**
   * This method contains the code that will be executed by a node.
   * Any uncaught {@link java.lang.Throwable Throwable} will be stored in the task via a call to {@link org.jppf.node.protocol.Task#setThrowable(java.lang.Throwable) Task.setThrowable(Throwable)}.
   */
  @Override
  public void run() {
    String callableResult;
    
    String callResult = "Undefined";
    try {
    	callResult = compute(isInNode() ? new NodeCallable() : new ClientCallable());
    } catch (Exception e) {
    	setThrowable(e);
    }
    
    
    setResult(callResult);
  }
  
  public static class NodeCallable implements JPPFCallable<String> {
	  @Override
	  public String call() throws Exception {
		  return "Executed in node.";
	  }
  }
  
  public static class ClientCallable implements JPPFCallable<String> {
	  @Override
	  public String call() throws Exception {
		  return "Executed in client.";
	  }
  }
}
