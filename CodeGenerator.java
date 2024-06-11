package rs.ac.bg.etf.pp1;


import java.util.*;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	//private Map<String,Integer> arraySize = new LinkedHashMap<String,Integer>();
	
	public CodeGenerator(){
		//chr
		Tab.chrObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
	
		//ord
		Tab.ordObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
	
		//len
		Tab.lenObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
		
	}
	
	public void visit(MethodDeclList methodDecl){
		if(!this.haveReturn){
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
		else
			this.haveReturn = false;
		
	}
	
	public void visit(MethodName methodName){
		if(methodName.getMethodName().equalsIgnoreCase("main")){
			mainPc = Code.pc;
		}
		methodName.obj.setAdr(Code.pc);
		Code.put(Code.enter);
		if(SemanticPass.methodParsCount.get(methodName.getMethodName()) == null){
			Code.put(0);
			if(SemanticPass.methodLocalsCount.get(methodName.getMethodName()) == null){
				Code.put(0);
			}
			else{
				Code.put(
						SemanticPass.methodLocalsCount.get(methodName.getMethodName()).intValue());
			}
			
		}
		else{
			Code.put(SemanticPass.methodParsCount.get(methodName.getMethodName()).intValue());
			if(SemanticPass.methodLocalsCount.get(methodName.getMethodName()) == null)
				Code.put(SemanticPass.methodParsCount.get(methodName.getMethodName()).intValue());
			else
				Code.put(SemanticPass.methodParsCount.get(methodName.getMethodName()).intValue() +
						SemanticPass.methodLocalsCount.get(methodName.getMethodName()).intValue());
		}
			
		
	}
	
	public void visit(DesignationActPars desactpa){
		DesignatorStatement desst = (DesignatorStatement) desactpa.getParent();
		Obj desObj = desst.getDesignator().getDesignatorName().obj;
		
		int offset = desObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	//public void visit(DesignationActPars )
	public void visit(AddOperationTermList addop){

	}
	
	public void magicFunction(Factor fact){
		if(fact.getParent() instanceof MulOperationFactorList){
			
			MulOperationFactorList mulloplist = (MulOperationFactorList) fact.getParent();
			MulOp mullop = (MulOp) mulloplist.getMulOp();
			if(mullop instanceof MulOpMul){
				Code.put(Code.mul);
			}
			else
				if(mullop instanceof MulOpDiv){
					Code.put(Code.div);
				}
				else{
					Code.put(Code.rem);
				}
			
			if(mulloplist.getMulOprFactorList() instanceof NoMulOperationFactorList){
				SyntaxNode number1 = fact;
				while(true){
					
					if(number1 instanceof Term){
						break;
					}
					
					number1 = number1.getParent();
				}
				
				if(number1.getParent() instanceof AddOperationTermList){
					
					AddOperationTermList addoplist1 = (AddOperationTermList) number1.getParent();
					AddOp addop = addoplist1.getAddOp();
					
					if(addop instanceof AddOpPlus){
						Code.put(Code.add);
					}
					else{
						Code.put(Code.sub);
					}
					
				}
				
			}
		}
		else{
			if(fact.getParent().getParent() instanceof AddOperationTermList){
				Term term2 = (Term) fact.getParent();
				if(term2.getMulOprFactorList() instanceof MulOperationFactorList) return;
				
				AddOperationTermList addoplist = (AddOperationTermList) fact.getParent().getParent();
				AddOp addop = addoplist.getAddOp();
				
				if(addop instanceof AddOpPlus){
					Code.put(Code.add);
				}
				else{
					Code.put(Code.sub);
				}
			}
		}
	}
	public void visit(DesignatorStatement desst){
		
		if(desst.getDesignation() instanceof DesignationAssignop){
			DesignationAssignop temp = (DesignationAssignop) desst.getDesignation();
			
				
				boolean isArray = false;
				Obj desObj = desst.getDesignator().getDesignatorName().obj;
				if(desObj.getType().getKind() == Struct.Array){
					
					if(desst.getDesignator().getDesignatorBrackets() instanceof DesignatorBracketsExpr){
						Code.put(Code.astore);
					}
					else{
						Code.store(desst.getDesignator().getDesignatorName().obj);
					}
					
				}
				else
					Code.store(desst.getDesignator().getDesignatorName().obj);
				
				
			
		}
		else
			if(desst.getDesignation() instanceof DesignationInc 
					|| desst.getDesignation() instanceof DesignationDec ){
				
				Obj desObj = desst.getDesignator().getDesignatorName().obj;
				if(desObj.getType().getKind() == Struct.Array){
					
					if(desst.getDesignator().getDesignatorBrackets() instanceof DesignatorBracketsExpr){
						Code.put(Code.astore);
					}
					else{
						Code.store(desst.getDesignator().getDesignatorName().obj);
					}
					
				}
				else
					Code.store(desst.getDesignator().getDesignatorName().obj);	
					
				
			}
		
	}
	
	public void factorCheck(Factor fact){
		if(fact.getParent() instanceof MulOperationFactorList){
			MulOperationFactorList mullist = (MulOperationFactorList) fact.getParent();
			if(mullist.getMulOp() instanceof MulOpMul){
				Code.put(Code.mul);
			}
			else
				if(mullist.getMulOp() instanceof MulOpDiv){
					Code.put(Code.div);
				}
				else{
					Code.put(Code.rem);
				}
		}
	}
	
	public void visit(FactorDesignator factDes){
		
		if(factDes.getSomeActParList() instanceof SomeActParametersList){
			
			Obj desObj = factDes.getDesignator().getDesignatorName().obj;
			
			int offset = desObj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset);
		}
		
	}
	public void visit(FactorNumber number){
		Code.loadConst(number.getFactorNumber().intValue());
		//this.magicFunction(number);
		this.factorCheck(number);
	}
	public void visit(FactorCharConst charr){
		Code.loadConst(charr.getFactorCharConst().charValue());
		//this.magicFunction(charr);
		this.factorCheck(charr);
	}
	public void visit(FactorBooleanConst bool){
		int k = 0;
		if(bool.getFactorBoolConst().booleanValue())
				k = 1;
		else 
			k = 0;
		
		Code.loadConst(k);
		//this.magicFunction(bool);
		this.factorCheck(bool);
	}
	
	public void visit(FactorExpr factExpr){
		//this.magicFunction(factExpr);
		this.factorCheck(factExpr);
	}
	
	public void visit(DesignatorName desName){
		Designator des = (Designator) desName.getParent();
		if(des.getDesignatorBrackets() instanceof DesignatorBracketsExpr){
			
			Code.load(desName.obj);
		}
	}
	public void visit(Designator designator){
		Obj desObj = designator.getDesignatorName().obj;
		if(designator.getParent() instanceof FactorDesignator){
	
		
			if(desObj.getKind() == Obj.Con){
				Code.load(desObj);
				//this.magicFunction((FactorDesignator) designator.getParent());
				this.factorCheck((FactorDesignator) designator.getParent());
			}
			else{
				
				if(desObj.getKind() == Obj.Var){
					
					if(desObj.getType().getKind() == Struct.Array){
						
						if(designator.getDesignatorBrackets() instanceof DesignatorBracketsExpr){
							Code.put(Code.aload);
							//this.magicFunction((FactorDesignator) designator.getParent());
							this.factorCheck((FactorDesignator) designator.getParent());
						}
						else{
							Code.load(desObj);
							
						}
					}
					else{
						Code.load(desObj);
						//this.magicFunction((FactorDesignator) designator.getParent());
						this.factorCheck((FactorDesignator) designator.getParent());
					}
					
				}
				
				else{
					

					
				}
				
			}
			
			
			
		}
		else{
			
		}
		
		
	}
	
	public void visit(Term term){
		if(term.getParent() instanceof Expr){
			Expr expr = (Expr) term.getParent();
			if(expr.getMinus() instanceof IfMinus)
				Code.put(Code.neg);
			
		}
		else{
			
			if(term.getParent() instanceof AddOperationTermList){
				AddOperationTermList addlist = (AddOperationTermList) term.getParent();
				
				if(addlist.getAddOp() instanceof AddOpPlus){
					Code.put(Code.add);
				}
				else{
					Code.put(Code.sub);
				}
				
			}
			
		}
		
		
		
	}
	public void visit(FactorNew factNew){
		
		Code.put(Code.newarray);
		Code.put(1);
	}
	
	public void visit(PrintSt printst){
		if(printst.getExpr().struct == Tab.intType){
			Code.loadConst(5);
			Code.put(Code.print);
		}
		else{
			if(printst.getExpr().struct == Tab.charType){
				Code.loadConst(1);
				Code.put(Code.bprint);
			}
			else{
				Code.loadConst(5);
				Code.put(Code.print);
			}
		}
		
	}
	
	public void visit(DesignationInc desInc){
		DesignatorStatement dest = (DesignatorStatement) desInc.getParent();
		Obj desObj = dest.getDesignator().getDesignatorName().obj;
		
		if(dest.getDesignator().getDesignatorBrackets() instanceof DesignatorBracketsExpr){
			Code.put(Code.dup2);
			Code.put(Code.aload);
			Code.put(Code.const_1);
			Code.put(Code.add);
		}
		else{
			Code.load(desObj);
			Code.put(Code.const_1);
			Code.put(Code.add);
		}
		
		
		
	}
	
	public void visit(DesignationDec desDec){
		DesignatorStatement dest = (DesignatorStatement) desDec.getParent();
		Obj desObj = dest.getDesignator().getDesignatorName().obj;
		
		if(dest.getDesignator().getDesignatorBrackets() instanceof DesignatorBracketsExpr){
			Code.put(Code.dup2);
			Code.put(Code.aload);
			Code.put(Code.const_1);
			Code.put(Code.sub);
		}
		else{
			Code.load(desObj);
			Code.put(Code.const_1);
			Code.put(Code.sub);
		};
		
	}
	
	public void visit(ReadSt readst){
		Obj desObj = readst.getDesignator().getDesignatorName().obj;
		
		if(desObj.getKind() == Obj.Var){
			
			if(desObj.getType().getKind() == Struct.Array){
				
				if(readst.getDesignator().getDesignatorBrackets() instanceof DesignatorBracketsExpr){
					Code.put(Code.read);
					Code.put(Code.astore);
				}
				
			}
			else{
				Code.put(Code.read);
				Code.store(desObj);
			}
			
		}
	}
	private boolean haveReturn = false;
	public void visit(ReturnSt ret){
		this.haveReturn = true;
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private int mainPc;
	
	public int getMainPc(){
		return mainPc;
	}
	
	// 2 6
	public void visit(DesignatorFindAny findAny){
		int cnt = 0;
		int marker1 = 0, marker2 = 0, marker3 = 0;
		DesignatorStatement desSt = (DesignatorStatement) findAny.getParent().getParent();
		DesignationAssignop desasop = (DesignationAssignop) desSt.getDesignation();
		FactorDesignator desfact = (FactorDesignator) desasop.getExpr().getTerm().getFactor();
		Obj desObj = desfact.getDesignator().getDesignatorName().obj;
		
		Code.loadConst(0);
		Code.put(Code.dup2);
		
		Code.load(desObj);
		Code.put(Code.arraylength);
		
		Code.putFalseJump(Code.ne, 0);
		marker1 = Code.pc - 2;
		
		Code.put(Code.dup2);
		Code.put(Code.pop);
		Code.load(desObj);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		Code.put(Code.aload);
		Code.putFalseJump(Code.ne, 0);
		marker2 = Code.pc - 2;
		
		Code.loadConst(1);
		Code.put(Code.add);
		Code.put(Code.dup2);
		Code.load(desObj);
		Code.put(Code.arraylength);
		Code.putJump(marker1 - 1);
		
		Code.fixup(marker1);
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.loadConst(0);
		
		Code.loadConst(1);
		Code.loadConst(1);
		
		Code.putFalseJump(Code.ne, 0);
		marker3 = Code.pc - 2;
		
		Code.fixup(marker2);
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.loadConst(1);
		
		Code.fixup(marker3);
		
	}
	
	public void visit(DesignatorCount count){
		DesignatorStatement desSt = (DesignatorStatement) count.getParent().getParent();
		DesignationAssignop desasop = (DesignationAssignop) desSt.getDesignation();
		FactorDesignator desfact = (FactorDesignator) desasop.getExpr().getTerm().getFactor();
		Obj desObj = desfact.getDesignator().getDesignatorName().obj;
		Obj cnt = Tab.find("cnt");
		int marker1 = 0, marker2 = 0;
		
		Code.loadConst(0);
		Code.put(Code.dup2);
		Code.load(desObj);
		Code.put(Code.arraylength);
		
		Code.putFalseJump(Code.ne, 0);
		marker1 = Code.pc - 2;
		
		Code.put(Code.dup2);
		Code.put(Code.pop);
		Code.load(desObj);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		Code.put(Code.aload);
		
		Code.putFalseJump(Code.eq, 0);
		marker2 = Code.pc - 2;
		
		Code.load(cnt);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(cnt);
		
		Code.fixup(marker2);
		
		Code.loadConst(1);
		Code.put(Code.add);
		Code.put(Code.dup2);
		Code.load(desObj);
		Code.put(Code.arraylength);
		Code.putJump(marker1 - 1);
		
		Code.fixup(marker1);
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		
		Code.load(cnt);
		Code.loadConst(0);
		Code.store(cnt);
		
		//Code.put(Code.trap);
	}
}
