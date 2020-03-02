package analysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import reporting.Reporter;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Class implementing dataflow analysis
 */
public class IntraproceduralAnalysis extends ForwardFlowAnalysis<Unit, Set<FlowAbstraction>> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public int flowThroughCount = 0;
	private final SootMethod method;
	private final Reporter reporter;

	public IntraproceduralAnalysis(Body b, Reporter reporter, int exercisenumber) {
		super(new ExceptionalUnitGraph(b));
		this.method = b.getMethod();
		this.reporter = reporter;

		logger.info("Analyzing method " + b.getMethod().getSignature() + "\n" + b);
	}

	@Override
	protected void flowThrough(Set<FlowAbstraction> taintsIn, Unit d, Set<FlowAbstraction> taintsOut) {
		Stmt s = (Stmt) d;
		logger.info("Unit " + d);
		if (d instanceof JAssignStmt) {
			JAssignStmt assignStmt = (JAssignStmt) d;
			if ((assignStmt.getRightOpBox().getValue() instanceof JSpecialInvokeExpr)
					&& (assignStmt.getRightOpBox().getValue().toString().contains("getSecret"))) {
				FlowAbstraction flowAbstraction = new FlowAbstraction(d, (Local) assignStmt.getLeftOpBox().getValue());
				taintsOut.add(flowAbstraction);

			} else if ((assignStmt.getRightOpBox().getValue() instanceof JimpleLocal)) {
				Local rightVal = (Local) assignStmt.getRightOpBox().getValue();
				if (taintsIn.iterator().next().getLocal() == rightVal) {
					FlowAbstraction flowAbstraction = new FlowAbstraction(d,
							(Local) assignStmt.getLeftOpBox().getValue());
					taintsOut.add(flowAbstraction);
				}
			}
			
			/* This is the code to remove the taint if the right side of the statement is constant
			and left side is local but I am not sure why it is not working*/
			
			else if ((assignStmt.getRightOpBox().getValue() instanceof StringConstant)
					&& (assignStmt.getLeftOpBox().getValue() instanceof JimpleLocal)) {
				// Local leftVal = (Local) assignStmt.getLeftOpBox().getValue();
				// if(taintsIn.iterator().next().getLocal() == leftVal) {
				// taintsIn.iterator().remove();
				// }
			}
		}
		if (d instanceof JInvokeStmt) {
			JInvokeStmt jInvokeStmt = (JInvokeStmt) d;
			SootMethod method = jInvokeStmt.getInvokeExpr().getMethod();
			if (method.getName().equals("leak")) {
				JimpleLocal arg = (JimpleLocal) jInvokeStmt.getInvokeExpr().getArg(0);

				Iterator<FlowAbstraction> it = taintsIn.iterator();
				while (it.hasNext()) {
					FlowAbstraction fab = it.next();
					if (fab.getLocal().getName().equals(arg.getName())) {
						reporter.report(this.method, fab.getSource(), d);
					}
				}
			}
		}
		Iterator<FlowAbstraction> it = taintsIn.iterator();
		while (it.hasNext()) {
			taintsOut.add(it.next());
		}
		/* IMPLEMENT YOUR ANALYSIS HERE */

		// reporter.report(this.method, fa.getSource(), d);
	}

	@Override
	protected Set<FlowAbstraction> newInitialFlow() {
		return new HashSet<FlowAbstraction>();
	}

	@Override
	protected Set<FlowAbstraction> entryInitialFlow() {
		return new HashSet<FlowAbstraction>();
	}

	@Override
	protected void merge(Set<FlowAbstraction> in1, Set<FlowAbstraction> in2, Set<FlowAbstraction> out) {
		out.addAll(in1);
		out.addAll(in2);
	}

	@Override
	protected void copy(Set<FlowAbstraction> source, Set<FlowAbstraction> dest) {
		dest.clear();
		dest.addAll(source);
	}

	public void doAnalyis() {
		super.doAnalysis();
	}

}
