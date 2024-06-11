package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

import java.io.*;
import java.util.*;

import javafx.util.Pair;

public class SemanticPass extends VisitorAdaptor {

	
	public static int nVars = 0;

	public boolean errorDetected = false;
	
	private Stack<Boolean> noBracketStack = new Stack<Boolean>();
	private Stack<Pair<Integer, Integer>> constantValueStack = new Stack<Pair<Integer, Integer>>();
	public static Map<String,Integer> methodParsCount = new LinkedHashMap<String,Integer>();
	public static Map<String,Integer> methodLocalsCount = new LinkedHashMap<String,Integer>();
	
	private int printCallCount = 0;
	private int varDeclCount = 0;
	private int currentLevelSem = -1;

	private int currentConstValue = 0;

	private Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		this.errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
		this.currentLevelSem++;
	}

	private Struct currentType = Tab.noType; // cuva se trenutna vrednost bilo
												// cega drugog
	private Struct currentReturnType = Tab.noType; // cuva se trenutna povratna
													// vrednost metode
	private int nMethodVars = 0;

	public void visit(Type type) {
		if (!this.errorDetected) {
			FactorNew tmpNew;

			if (type.getParent() instanceof MethodType) {
				
				Obj findMethodType = Tab.find(type.getVarType());
				if (findMethodType.getType() == Tab.noType) {
					this.report_error("Povratna vrednost metode je neispravna!"
							+ type.getVarType() + " u tabeli simbola!", null);
					this.errorDetected = true;
				} else {
					this.currentReturnType = findMethodType.getType();
				}

			} else {
				Obj findType = Tab.find(type.getVarType());
				if (findType == Tab.noObj) {
					this.report_error("Nije pronadjen tip " + type.getVarType()
							+ " u tabeli simbola!", null);
				} else {
					if (type.getParent() instanceof FactorNew) {
						tmpNew = (FactorNew) type.getParent();
						tmpNew.struct = findType.getType();
					}
					this.currentType = findType.getType();
				}
			}

		}
	}

	public void visit(VarDeclName varName){
		
	}
	public void visit(VarDecalarationList varDeclList) {
		if (!this.errorDetected) {
			Obj findDecl = Tab.find(varDeclList.getVarDeclName().getVarName());
			Obj p;
			if (findDecl != Tab.noObj
					&& findDecl.getLevel() == this.currentLevelSem) {
				this.report_error(
						"Vec postoji promenljiva sa istim imenom na trenutnom nivou!",
						null);
				this.errorDetected = true;
			} else {
				if (!this.noBracketStack.pop())
					p = Tab.insert(Obj.Var, varDeclList.getVarDeclName().getVarName(),
							this.currentType);				
				else
					p = Tab.insert(Obj.Var, varDeclList.getVarDeclName().getVarName(), new Struct(3,
							this.currentType));
				//this.report_info(varDeclList.getVarDeclName().getVarName(), null);

				if(p.getLevel() == 0){
					nVars++;
				}
				
				varDeclList.getVarDeclName().obj = p;
				VarDecalarationList temp1 = varDeclList;
				
				while(true){
					if(temp1.getParent().getParent() instanceof TypeDeclList) break;
					if(temp1.getParent().getParent() instanceof MethodVarDecl){
						
						Integer count = SemanticPass.methodLocalsCount.get(this.currentMethodName);
						if(count == null){
							SemanticPass.methodLocalsCount.put(this.currentMethodName, 1);
						}
						else{
							SemanticPass.methodLocalsCount.put(this.currentMethodName, count + 1);
						}
						// dodavanje lokalne promenljive
						
						break;
					}
					else
						temp1 = (VarDecalarationList) temp1.getParent();
					
				}
				
				
				
				
			}
		}
	}

	public void visit(VarDecl varDecl) {
		if (!this.errorDetected) {
			Obj p = Tab.noObj;
			Obj findDecl = Tab.find(varDecl.getVarDeclName().getVarName());
			if (findDecl != Tab.noObj
					&& findDecl.getLevel() == this.currentLevelSem) {
				this.report_error(
						"Vec postoji promenljiva sa istim imenom na trenutnom nivou!",
						null);
				this.errorDetected = true;
			} else {
				
				if (!this.noBracketStack.pop())
					p = Tab.insert(Obj.Var, varDecl.getVarDeclName().getVarName(),
							this.currentType);
				else
					p = Tab.insert(Obj.Var, varDecl.getVarDeclName().getVarName(), new Struct(3,
							this.currentType));

				//this.report_info(varDecl.getVarDeclName().getVarName(), null);

				varDecl.getVarDeclName().obj = p;
				if(varDecl.getParent() instanceof MethodVarDeclaration){
					Integer count = SemanticPass.methodLocalsCount.get(this.currentMethodName);
					if(count == null){
						SemanticPass.methodLocalsCount.put(this.currentMethodName, 1);
					}
					else{
						SemanticPass.methodLocalsCount.put(this.currentMethodName, count + 1);
					}
					// dodavanje lokalne promenljive
					
				}
				if(p.getLevel() == 0){
					nVars++;
				}
			}
		}
	}

	public void visit(EmptyBrackets brackets) {
		if (!this.errorDetected) {
			this.noBracketStack.push(true);
		}
	}

	public void visit(NoEmptyBrackets brackets) {
		if (!this.errorDetected) {
			this.noBracketStack.push(false);
		}
	}

	public void visit(ConstDecl constDecl) {
		if (!this.errorDetected) {
			int type = 0;
			switch (this.currentType.getKind()) {
			case 1:
				type = 1;
				break;
			case 2:
				type = 2;
				break;
			case 5:
				type = 5;
				break;
			}
			Pair<Integer, Integer> constVal = this.constantValueStack.pop();
			if (type != constVal.getValue().intValue()) {
				this.report_error("Tip i vrednost konstante se ne podudaraju!",
						null);
				this.errorDetected = true;
			} else {
				Obj findDecl = Tab.find(constDecl.getConstantName().getConstName());
				if (findDecl != Tab.noObj
						|| findDecl.getLevel() == this.currentLevelSem) {
					this.report_error(
							"Vec postoji promenljiva sa istim imenom!", null);
					this.errorDetected = true;
				} else {
					Obj obj = Tab.insert(Obj.Con, constDecl.getConstantName().getConstName(),
							this.currentType);
					obj.setAdr(constVal.getKey());

					constDecl.getConstantName().obj = obj;
				}
			}
		}
	}

	public void visit(ConstDeclarationList constDeclList) {
		if (!this.errorDetected) {
			int type = 0;
			switch (this.currentType.getKind()) {
			case 1:
				type = 1;
				break;
			case 2:
				type = 2;
				break;
			case 5:
				type = 5;
				break;
			}
			Pair<Integer, Integer> constVal = this.constantValueStack.pop();
			if (type != constVal.getValue().intValue()) {
				this.report_error("Tip i vrednost konstante se ne podudaraju!",
						null);
				this.errorDetected = true;
			} else {

				Obj findDecl = Tab.find(constDeclList.getConstantName().getConstName());
				if (findDecl != Tab.noObj
						|| findDecl.getLevel() == this.currentLevelSem) {
					this.report_error(
							"Vec postoji promenljiva sa istim imenom!", null);
					this.errorDetected = true;
				} else {
					Obj obj = Tab.insert(Obj.Con, constDeclList.getConstantName().getConstName(),
							this.currentType);
					obj.setAdr(constVal.getKey());
					constDeclList.getConstantName().obj = obj;
				}

			}
		}
	}

	public void visit(ConstantNumber constant) {
		if (!this.errorDetected) {
			Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
					constant.getConstValue(), 1);
			this.constantValueStack.push(pair);
		}
	}

	public void visit(ConstantBoolean constant) {
		if (!this.errorDetected) {
			Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
					Boolean.parseBoolean(constant.getConstValue().toString()) == true ? 1
							: 0, 5);
			this.constantValueStack.push(pair);
		}
	}

	public void visit(ConstantChar constant) {
		if (!this.errorDetected) {
			Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
					(int) constant.getConstValue().toString().charAt(0), 2);
			this.constantValueStack.push(pair);
		}
	}

	public void visit(Program program) {
		if (!this.errorDetected) {
			Tab.chainLocalSymbols(program.getProgName().obj);
			Tab.closeScope();
			this.currentLevelSem--;
			
			if(this.allMethodNames.get("main") == null){
				
				this.report_error("No main method delcared!", null);
				this.errorDetected = true;
				return;
				
			}
			
		}
	}

	private boolean isMethodType = false;
	private Map<String, Integer> allMethodNames = new LinkedHashMap<String, Integer>();
	private boolean hasReturn = false;
	public void visit(MethodDeclList method) {
		if(!this.errorDetected){
			if (method.getMethodName().obj != null) {
				if(method.getMethodName().obj.getType() != Tab.noType && !this.hasReturn){
					this.report_error("This method has no return!", null);
					this.errorDetected = true;
					return;
				}
				this.hasReturn = false;
				method.getMethodName().obj.setLevel(this.nMethodVars);
				this.nMethodVars = 0;
				Tab.chainLocalSymbols(method.getMethodName().obj);
				this.currentMethodName = "";
				Tab.closeScope();
				this.currentLevelSem--;
			}
			else{
				this.report_error("Fatal Error!", null);
				this.errorDetected = true;
				return;
			}
		}
		
	}

	private boolean inMethodDeclaration = false;

	public void visit(NoMethodVarDeclaration noMoreMethodVars) {
		if (!this.errorDetected) {
			this.inMethodDeclaration = false;
		}
	}

	private String currentMethodName = "";
	public void visit(MethodName methodName) {
		if (!this.errorDetected) {
			if (this.allMethodNames.get(methodName.getMethodName()) != null) { // moze
																				// da
																				// se
																				// doda
																				// provera
																				// za
																				// nivoe
																				// za
																				// C
																				// nivo
				this.report_error("Vec postoji metoda sa istim imenom!", null);
				this.errorDetected = true;
			} else {
				this.inMethodDeclaration = true;
				methodName.obj = Tab.insert(Obj.Meth,
						methodName.getMethodName(), this.currentReturnType);
				this.allMethodNames.put(methodName.getMethodName(), 1);
				this.currentMethodName = methodName.getMethodName();
				Tab.openScope();
				this.currentLevelSem++;

			}
		}
	}

	public void visit(ParameterName parName){
		
		Obj foundDeclaration = Tab.find(parName.getParameterName());
		Obj p = null;
		if (foundDeclaration == Tab.noObj
				|| foundDeclaration.getLevel() != this.currentLevelSem) {
			this.nMethodVars++;
			if(parName.getParent() instanceof FormParametersList){
				FormParametersList formparlist = (FormParametersList) parName.getParent();
				if(formparlist.getBrackets() instanceof EmptyBrackets){
					p = Tab.insert(Obj.Var, parName.getParameterName(),
							new Struct(Struct.Array, this.currentType));
				}
				else{
					p = Tab.insert(Obj.Var, parName.getParameterName(),
							this.currentType);
				}
			}
			else{
				FormParameters formpars = (FormParameters) parName.getParent();
				if(formpars.getBrackets() instanceof EmptyBrackets){
					p = Tab.insert(Obj.Var, parName.getParameterName(),
							new Struct(Struct.Array, this.currentType));
				}
				else{
					p = Tab.insert(Obj.Var, parName.getParameterName(),
							this.currentType);
				}
			}
			
		} else {
			this.report_error(
					"Vec postoji promenljiva sa istim imenom na trenutnom nivou!",
					null);
			this.errorDetected = true;
		}
		
	}
	public void visit(FormParametersList formParamethersList) {
		if (!this.errorDetected) {

			Integer count = SemanticPass.methodParsCount.get(this.currentMethodName);
			if(count == null){
				SemanticPass.methodParsCount.put(this.currentMethodName, 1);
			}
			else{
				SemanticPass.methodParsCount.put(this.currentMethodName, count + 1);
			}
			// dodavanje parametra
		}

	}

	public void visit(FormParameters formParamethers) {
		if (!this.errorDetected) {
			
			Integer count = SemanticPass.methodParsCount.get(this.currentMethodName);
			if(count == null){
				SemanticPass.methodParsCount.put(this.currentMethodName, 1);
			}
			else{
				SemanticPass.methodParsCount.put(this.currentMethodName, count + 1);
			}
			// dodavanje parametra
			
		}
	}

	public void visit(TypeOfMethod methodType) {
		if (!this.errorDetected) {
			
		}
	}

	public void visit(VoidTypeOfMethdod methodType) {
		if (!this.errorDetected) {
			this.currentReturnType = Tab.noType;
		}
	}

	private Struct findAnyDesignatorType = Tab.noType;
	private Struct countDesignatorType = Tab.noType;
	private Struct findAndReplaceDesignatorType = Tab.noType;
	
	private Stack<Obj> stackDesignator = new Stack<Obj>();
	private Stack<Obj> stackDesignatorWithParameters = new Stack<Obj>();
	private Obj currentDesignator = Tab.noObj;
	private Stack<Integer> stackDesignatorWithParametersIndex = new Stack<Integer>();
	private int parIndex = 0;

	public void visit(Designator designator) {
		if (!this.errorDetected) {

			Obj desObj = this.stackDesignator.pop();
			FactorDesignator syntObj;
			Struct desType;
			desType = desObj.getType();
			if (designator.getDesignatorBrackets() instanceof DesignatorBracketsExpr) {
				if (desType.getKind() != Struct.Array) {
					this.report_error(
							"Ne moze se indeksirati promenljiva koja nije tipa niz!",
							null);
					this.errorDetected = true;
					return;
				} else {
					desType = desObj.getType().getElemType();
				}
			}

			if(designator.getParent() instanceof DesignatorStatement){
				DesignatorStatement methodCallDesignator =  (DesignatorStatement) designator.getParent();
				
				if(methodCallDesignator.getDesignation() instanceof DesignationActPars)
				{
					if(desObj.getKind() != Obj.Meth){
						this.report_error("Cant be invoked on anything other than method!", null);
						this.errorDetected = true;
						return;
					}
					else if(desObj.getType() != Tab.noType){
						this.report_error("Procedure can only be called on void type methods!", null);
						this.errorDetected = true;
						return;
					}
					else {
						this.stackDesignatorWithParameters.push(desObj);
						this.stackDesignatorWithParametersIndex.push(0);
						this.currentDesignator = Tab.noObj;
						this.parIndex = 0;
					}
				}
			}
			else
				/*if(designator.getParent() instanceof DesignatorFindAny){
					if(!designator.getDesignatorName().getDesignatorName().equals("findAny")){
						this.report_error("Error, findAny not called!", null);
						this.errorDetected = true;
						return;
					}
				}
				else*/
				if (designator.getParent() instanceof FactorDesignator) {
					syntObj = (FactorDesignator) designator.getParent();
					syntObj.struct = desType;
	
					if (syntObj.getSomeActParList() instanceof SomeActParametersList) {
						if (desObj.getKind() != Obj.Meth) {
							this.report_error("Tip promenljive nije metoda!", null);
						} else {
							// resetovanje za nove parametre
							if (this.currentDesignator != Tab.noObj) {
								this.stackDesignatorWithParameters
										.push(this.currentDesignator);
								this.stackDesignatorWithParametersIndex
										.push(this.parIndex);
	
							}
							this.stackDesignatorWithParameters.push(desObj);
							this.stackDesignatorWithParametersIndex.push(0);
							this.currentDesignator = Tab.noObj;
							this.parIndex = 0;
						}
					}
				} 
				else {
					/*if (designator.getParent() instanceof DesignatorFindAny) {
						if (!desObj.getName().equals("findAny")) {
							this.report_error("Mehtodname is not findAny", null);
						}
					} else {
						if (designator.getParent() instanceof DesignatorFindAnyAndReplace) {
							if (!desObj.getName().equals("findAndReplace")) {
								this.report_error("Mehtodname is not findAny", null);
							}
						}
					}*/
				}

			designator.struct = desType;
			
		}

	}

	public void visit(DesignatorName designatorName) {
		if (!this.errorDetected) {
			Obj foundDesignator = Tab.find(designatorName.getDesignatorName());
			if (foundDesignator == Tab.noObj) {

				this.report_error("Promenjljiva ne postoji u tabli simbola!",
						null);
				this.errorDetected = true;
			} else {
				this.stackDesignator.push(foundDesignator);
				designatorName.obj = foundDesignator;
			}
		}

	}

	private Struct currentExprType = Tab.noType;

	public void visit(Expr expr) {
		if (!this.errorDetected) {
			Object[] parameterArray;
			DesignationAssignop exprDesignator = null;
			FactorDesignator factDes = null;
			if (expr.struct == null) {
				this.report_error("nesto ne valja", null);
				this.errorDetected = true;
			} else {
				
				if(expr.getMinus() instanceof IfMinus && expr.struct != Tab.intType){
					this.report_error("Negative expression must be type of int", null);
					this.errorDetected = true;
					return;
				}
				
				if(expr.getParent() instanceof PrintSt){
					
					if(expr.getTerm().getFactor() instanceof FactorNew){
						
						this.report_error("Cannot print array!", null);
						this.errorDetected = true;
						return;
					}
					else{
						if(expr.struct.getKind() != Struct.Int
							&& expr.struct.getKind() != Struct.Bool
							&& expr.struct.getKind() != Struct.Char){
							this.report_error("Wrong type of expression in print statement!", null);
							this.errorDetected = true;
							return;
						}
					}
					
				}
				else if(expr.getParent() instanceof FactorNew){
					if(expr.getTerm().getFactor() instanceof FactorNew || expr.struct != Tab.intType){
						this.report_error("Size of new array can only be int value!", null);
						this.errorDetected = true;
						return;
					}
				
				}
				
				else if(expr.getParent() instanceof DesignationAssignop){
				
					exprDesignator = (DesignationAssignop) expr.getParent();
					if( !(exprDesignator.getDesignatorFunctions() instanceof NoDesignatorFunctions))
					{
						if(
							exprDesignator.getExpr().getTerm().getFactor() instanceof FactorDesignator
							&&
							exprDesignator.getExpr().getAddOpTermList() instanceof NoAddOperationTermList
						    &&
							exprDesignator.getExpr().getTerm().getMulOprFactorList() instanceof NoMulOperationFactorList
							&&
							exprDesignator.getExpr().getMinus() instanceof NoIfMinus
						){
							if(exprDesignator.getDesignatorFunctions() instanceof DesignatorFindAny){
								if(exprDesignator.getExpr().struct.getKind() != Struct.Array){
									this.report_error("findAny can be called only upon array!", null);
									this.errorDetected = true;
									return;
								}
								else{
									this.findAnyDesignatorType = expr.struct.getElemType();
								}
							}
							else if(exprDesignator.getDesignatorFunctions() instanceof DesignatorCount){
								if(exprDesignator.getExpr().struct.getKind() != Struct.Array){
									this.report_error("count can be called only upon array!", null);
									this.errorDetected = true;
									return;
								}
								else{
									this.countDesignatorType = expr.struct.getElemType();
								}
							}
						}
						else{
							this.report_error("findAny or findAndReplace can be called only upon array", null); 
							this.errorDetected = true;
							return;
						}
						
					}
				
				
				}
				else 
					if(expr.getParent() instanceof DesignatorFindAny){
						if(this.findAnyDesignatorType != expr.struct){
							this.report_error("Type of array element does not match array type in findAny!", null);
							this.errorDetected = true;
							return;
						}
					}
					else
						if(expr.getParent() instanceof DesignatorCount){
							if(this.countDesignatorType != expr.struct){
								this.report_error("Type of array element does not match array type in count!", null);
								this.errorDetected = true;
								return;
							}
						}					
						else{
							if (expr.getAddOpTermList() instanceof AddOperationTermList
								&& expr.struct != Tab.intType) {
								this.report_error("Izraz mora biti tipa int!", null);
								this.errorDetected = true;
								return;
			
							} 
							else {
								if (expr.getParent() instanceof FactorExpr) {
									Factor factExpr = (FactorExpr) expr.getParent();
									factExpr.struct = expr.struct;
								}
								else{
									if(expr.getParent() instanceof ActPars || expr.getParent() instanceof ActParametersList){
										
										if(this.currentDesignator == Tab.noObj){
											this.currentDesignator = this.stackDesignatorWithParameters.pop();
											this.parIndex = this.stackDesignatorWithParametersIndex.pop();
											
										}
										
										parameterArray = this.currentDesignator.getLocalSymbols().toArray();
										
										if(this.parIndex >= parameterArray.length){
											this.report_error("There are more parameters than arguments of a function!", null);
											this.errorDetected = true;
											return;
										}
										
										int i = 0;
										Obj foundArgument = null;
										Obj temp = null;
										
										while(i < methodParsCount.get(this.currentDesignator.getName())){ 
											temp = (Obj)parameterArray[i];
											
											if(temp.getAdr() == this.parIndex){
												foundArgument = (Obj) temp;
												break;
											}
											
											++i;
										}
									
										if(foundArgument != null && expr.struct != foundArgument.getType()){
											
											this.report_error("Argument and parameter are not the same type!", null);
											this.errorDetected = true;
											return;
										}
										else{
											++(this.parIndex);
										}
										
									}
									
								}	
							}
			  }
			}
		}
	}
	

	public void visit(FactorNumber factorNumber) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorNumber;

			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr = (Expr) tmp;

					if (tmpExpr.struct == null) {
						tmpExpr.struct = Tab.intType;
					} else {
						if (tmpExpr.struct != Tab.intType) {
							this.report_error(
									"Expression error, types not compatible!",
									null);
							this.errorDetected = true;
						}
					}
					break;
				} else
					tmp = tmp.getParent();

			}
		}
	}

	public void visit(FactorBooleanConst factorBoolean) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorBoolean;

			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr;
					tmpExpr = (Expr) tmp;
					if (tmpExpr.struct == null) {
						tmpExpr.struct = Tab.find("bool").getType();
					} else {
						/*
						 * if(tmpExpr.struct != Tab.find("bool").getType()){
						 * this.report_error
						 * ("Expression error, types not compatible!", null); }
						 */
						this.report_error(
								"Expression error, types not compatible!", null);
						this.errorDetected = true;
					}
					break;
				} else

					tmp = tmp.getParent();

			}
		}
	}

	public void visit(FactorCharConst factorCharacter) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorCharacter;

			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr = (Expr) tmp;

					if (tmpExpr.struct == null) {
						tmpExpr.struct = Tab.charType;
					} else {
						/*
						 * if(tmpExpr.struct != Tab.charType){
						 * this.report_error(
						 * "Expression error, types not compatible!" , null); }
						 */
						this.report_error(
								"Expression error, types not compatible!", null);
						this.errorDetected = true;
					}
					break;
				} else
					tmp = tmp.getParent();

			}
		}
	}

	public void visit(FactorDesignator factorDesignator) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorDesignator;
			Obj desObj = Tab.find(factorDesignator.getDesignator().getDesignatorName().getDesignatorName());
			
			if(desObj.getKind() == Obj.Meth && factorDesignator.getSomeActParList() instanceof NoSomeActParametersList){
				this.report_error("Wrong call of a function", null);
				this.errorDetected = true;
				return;
			}
			
			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr = (Expr) tmp;

					if (tmpExpr.struct == null) {
						tmpExpr.struct = factorDesignator.struct;
					} else {
						if (factorDesignator.struct != Tab.intType) {
							this.report_error(
									"Expression error, types not compatible!",
									null);
							this.errorDetected = true;
						}
					}
					break;
				} else
					tmp = tmp.getParent();

			}
		}

	}

	public void visit(FactorNew factorNew) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorNew;

			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr = (Expr) tmp;

					if (tmpExpr.struct == null) {
						tmpExpr.struct = new Struct(Struct.Array,
								factorNew.struct);
					} else {
						this.report_error(
								"Expression error, types not compatible!", null);
						this.errorDetected = true;
					}
					break;
				} else
					tmp = tmp.getParent();

			}
		}
	}

	public void visit(FactorExpr factorExpr) {
		if (!this.errorDetected) {
			SyntaxNode tmp = factorExpr;

			while (true) {

				if (tmp instanceof Expr) {
					Expr tmpExpr = (Expr) tmp;

					if (tmpExpr.struct == null) {
						tmpExpr.struct = factorExpr.struct;
					} else if (factorExpr.struct != Tab.intType) {
						this.report_error(
								"Expression error, types not compatible!", null);
						this.errorDetected = true;
					}

					break;
				} else
					tmp = tmp.getParent();

			}
		}

	}

	public void visit(DesignatorBracketsExpr arrayBrackets) {
		if (!this.errorDetected) {
			if (arrayBrackets.getExpr().struct != Tab.intType) {
				// greska o pogresnom indeksiranju
				this.report_error(
						"Vrednost indeksa mora biti celobrojna vrednost!", null);
				this.errorDetected = true;
			} else {

			}
		}
	}

	public void visit(DesignatorStatement designatorStatement) {
		if (!this.errorDetected) {
			DesignationAssignop temp1 = null;

			if (designatorStatement.getDesignation() instanceof DesignationAssignop){
				// if(designatorStatement.getDesignator())
				temp1 = (DesignationAssignop) designatorStatement
						.getDesignation();
				if (temp1.getDesignatorFunctions() instanceof NoDesignatorFunctions) {
					Obj desobj1 = Tab.find(designatorStatement.getDesignator().getDesignatorName().getDesignatorName());
					if(desobj1.getKind() == Obj.Con){
						this.report_error("Left side of assignment cant be a constant!", null);
						this.errorDetected = true;
						return;
					}
					
					if(desobj1.getKind() == Obj.Meth){
						this.report_error("Left side of assignment cant be a fucntion!", null);
						this.errorDetected = true;
						return;
					}
					
					if (temp1.getExpr().struct.getKind() == designatorStatement
							.getDesignator().struct.getKind()) {
	
						if (temp1.getExpr().struct.getKind() == Struct.Array) {
							if (temp1.getExpr().struct.getElemType() != designatorStatement
									.getDesignator().struct.getElemType()) {
								this.report_error(
										"Error, types of arrays are not same!", null);
								this.errorDetected = true;
								return;
							}
						} 
						
						this.currentDesignator = Tab.noObj;
						this.parIndex = 0;
						this.stackDesignatorWithParameters.clear();
						this.stackDesignatorWithParametersIndex.clear();
						
					} else {
						this.report_error("Error, types not comaptible!",
								null);
						this.errorDetected = true;
						return;
					}
				}
				else
					if(temp1.getDesignatorFunctions() instanceof DesignatorFindAny){
						Obj desobj = Tab.find(designatorStatement.getDesignator().getDesignatorName().getDesignatorName());
						
						if(desobj.getKind() == Obj.Con){
							this.report_error("Left side of assignment cant be a constant!", null);
							this.errorDetected = true;
							return;
						}
						
						if(desobj.getKind() != Obj.Var || desobj.getType() != Tab.find("bool").getType()){
							this.report_error("Designator type is not bool!", null); 
						}
					}
					else{
						if(temp1.getDesignatorFunctions() instanceof DesignatorCount){
							Obj desobj = Tab.find(designatorStatement.getDesignator().getDesignatorName().getDesignatorName());
							
							if(desobj.getKind() == Obj.Con){
								this.report_error("Left side of assignment cant be a constant!", null);
								this.errorDetected = true;
								return;
							}
							
							if(desobj.getType().getKind() == Struct.Array){
								
								if(desobj.getType().getElemType() != Tab.intType){
									this.report_error("Designator type is not int!", null); 
								}
								
							}
							else
								if(desobj.getKind() != Obj.Var || desobj.getType() != Tab.intType){
									
									this.report_error("Designator type is not int!", null); 
								}
						}
					}
						
			}
			else{
				
				if (designatorStatement.getDesignation() instanceof DesignationActPars){
					//nema nista ovde za sad
				}
				else{
					Obj desobj = Tab.find(designatorStatement.getDesignator().getDesignatorName().getDesignatorName()); 
					if(desobj.getKind() == Obj.Con){
						this.report_error("Left side of assignment cant be a constant!", null);
						this.errorDetected = true;
						return;
					}
					if(designatorStatement.getDesignation() instanceof DesignationInc){
						
						if(desobj.getKind() != Obj.Var || desobj.getType() != Tab.intType){
							if(desobj.getType().getKind() == Struct.Array){
								
								if(desobj.getType().getElemType() != Tab.intType){
									this.report_error("Icrement is only possible on int type variables!", null);
									this.errorDetected = true;
									return;
								}
								else{
									
									if(designatorStatement.getDesignator().getDesignatorBrackets() instanceof NoDesignatorBracketsExpr){
										this.report_error("Icrement is only possible on int type variables!", null);
										this.errorDetected = true;
										return;
									}
									
								}
								
							}
							else{
								this.report_error("Icrement is only possible on int type variables!", null);
								this.errorDetected = true;
								return;
							}
							
						}
					}
					
					else if(designatorStatement.getDesignation() instanceof DesignationDec){
						if(desobj.getKind() != Obj.Var || desobj.getType() != Tab.intType){
							if(desobj.getType().getKind() == Struct.Array){
								
								if(desobj.getType().getElemType() != Tab.intType){
									this.report_error("Decrement is only possible on int type variables!", null);
									this.errorDetected = true;
									return;
								}
								else{
									
									if(designatorStatement.getDesignator().getDesignatorBrackets() instanceof NoDesignatorBracketsExpr){
										this.report_error("Decrement is only possible on int type variables!", null);
										this.errorDetected = true;
										return;
									}
									
								}
								
							}
							else{
								this.report_error("Decrement is only possible on int type variables!", null);
								this.errorDetected = true;
								return;
							}
							
						}
					}
					
				}
				
				
			}
		}

	}

	public void visit(ActParsDesignator parList) {
		if (!this.errorDetected) {

			if (this.methodParsCount.get(
					this.currentDesignator.getName()) != this.parIndex) {
				this.report_error(
						"Number of arguments and paramteres are not the same!",
						null);
				this.errorDetected = true;
			} else {
				if (!this.stackDesignatorWithParameters.empty()) {
					this.currentDesignator = this.stackDesignatorWithParameters
							.pop();
					this.parIndex = this.stackDesignatorWithParametersIndex
							.pop();
				} else {
					this.currentDesignator = Tab.noObj;
					this.parIndex = 0;
				}

			}

		}
	}
	
	public void visit(ReadSt readst){
		if(!this.errorDetected){
			Obj desObj = Tab.find(readst.getDesignator().getDesignatorName().getDesignatorName());
			
			if(desObj.getKind() != Obj.Var
					&& desObj.getType() != Tab.intType
					&& desObj.getType() != Tab.charType
					&& desObj.getType() != Tab.find("bool").getType()){
				
				this.report_error("Wrong type of designator in read statement!", null);
				this.errorDetected = true;
				
			}
		}
	}

	public void visit(ReturnSt ret){
		if(!this.errorDetected){
			
			Obj method = Tab.find(this.currentMethodName);
			if(method.getType() == Tab.noType){
				if(ret.getReturnExpr() instanceof ReturnExpresion){
					this.report_error("Incompatible with return type1!", null);
					this.errorDetected = true;
					return;
				}
			
			}
			else{
				if(ret.getReturnExpr() instanceof ReturnExpresion){
					ReturnExpresion retExpr = (ReturnExpresion) ret.getReturnExpr();
					
					if(method.getType() != retExpr.getExpr().struct){
						this.report_error("Incompatible with return type!", null);
						this.errorDetected = true;
						return;
					}
					
				}
				else{
					this.report_error("Incompatible with return type!", null);
					this.errorDetected = true;
					return;
				}
			}
			this.hasReturn = true;
		}
	}
	
	public void visit(NoTypeDeclaration nodecl){
		if(!this.errorDetected){
			Tab.insert(Obj.Var, "cnt", Tab.intType);
		}
	}
}
