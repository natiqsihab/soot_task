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
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
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
			
			/* 
			 * Checking that right box is instance of JSpecialInvoke expression and  check if right side
			 * contains the method from which tainted value is coming. Add the left box value to a set of 
			 *tainted values 
			 */
			if ((assignStmt.getRightOpBox().getValue() instanceof JSpecialInvokeExpr)
					&& (assignStmt.getRightOpBox().getValue().toString().contains("getSecret"))) {
				FlowAbstraction flowAbstraction = new FlowAbstraction(d, (Local) assignStmt.getLeftOpBox().getValue());
				taintsOut.add(flowAbstraction);
			}
			/* 
			 * Check if right box is instance of JimpleLocal and this value is tainted
			 * then add left box value to tainted values. 
			 */
			 else if ((assignStmt.getRightOpBox().getValue() instanceof JimpleLocal)) {
				Local rightVal = (Local) assignStmt.getRightOpBox().getValue();
				if (taintsIn.iterator().next().getLocal() == rightVal) {
					if(assignStmt.getLeftOpBox().getValue() instanceof FieldRef) {
						FlowAbstraction flowAbstraction = new FlowAbstraction(d,
								(FieldRef) assignStmt.getLeftOpBox().getValue());
						taintsOut.add(flowAbstraction);
					}
					else{
						FlowAbstraction flowAbstraction = new FlowAbstraction(d,
								(Local) assignStmt.getLeftOpBox().getValue());
						taintsOut.add(flowAbstraction);	
						}	
				}
			}
			/* 
			 * Check if right box is instance of JInstanceFieldRef and this value is tainted
			 * then add left box value to tainted values. 
			 */	
			 else if((assignStmt.getRightOpBox().getValue() instanceof JInstanceFieldRef)) {
				FieldRef rightVal=(FieldRef) assignStmt.getRightOpBox().getValue();
					if(assignStmt.getLeftOpBox().getValue() instanceof FieldRef) {
						FlowAbstraction flowAbstraction = new FlowAbstraction(d,
								(FieldRef) assignStmt.getLeftOpBox().getValue());
						taintsOut.add(flowAbstraction);
					}
					else{
						FlowAbstraction flowAbstraction = new FlowAbstraction(d,
								(Local) assignStmt.getLeftOpBox().getValue());
						taintsOut.add(flowAbstraction);	
						}	
				
			}
			/*
			 *  Check if right box is instance of StringConstant, remove the left box value from 
			 * the tainted values.
			 */							
			else if ((assignStmt.getRightOpBox().getValue() instanceof StringConstant)
					&& (assignStmt.getLeftOpBox().getValue() instanceof JimpleLocal)) {
				 Local leftVal = (Local) assignStmt.getLeftOpBox().getValue();
				 if(!taintsIn.isEmpty()) {
					 if(taintsIn.iterator().next().getLocal() == leftVal) {
						 taintsIn.iterator().remove();
					 }
				 }
			}
		}
		
		/*
		 * Report the values that are tainted and passed as a parameter to other functions.
		 */
		if (d instanceof JInvokeStmt) {
			JInvokeStmt jInvokeStmt = (JInvokeStmt) d;
			SootMethod method = jInvokeStmt.getInvokeExpr().getMethod();
			if (method.getName().equals("leak")) {
				JimpleLocal arg = (JimpleLocal) jInvokeStmt.getInvokeExpr().getArg(0);

				for(Object object : taintsIn) {
					FlowAbstraction fab=(FlowAbstraction) object;
					
					if (fab.toString().contains(arg.getName())) {
						reporter.report(this.method, fab.getSource(), d);
					}
				}
			}
		}
		
		/*
		 * Adding all tainted Values to a set
		 */
		for(Object object : taintsIn) {
			taintsOut.add((FlowAbstraction) object);
		}
		
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
