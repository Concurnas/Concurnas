package com.concurnas.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.concurnas.compiler.ast.*;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.util.MactchCase;
import com.concurnas.runtime.Pair;

@SuppressWarnings("unchecked")
public class ASTCreator extends ConcurnasBaseVisitor<Object> {
	private final String sourceName;
	private final LexParseErrorCapturer parserErrors;

	public ASTCreator(String sourceName, LexParseErrorCapturer parserErrors) {
		this.sourceName = sourceName;
		this.parserErrors = parserErrors;
	}

	private Expression parseLongOrInt(int line, int col, String text) {
		try {
			long val;
			if (text.length() > 2 && text.charAt(1) == 'b') {// binary encoded
																// string, e.g.
																// 0b00100101
				val = Long.parseLong(text.substring(2), 2);
			} else {
				val = Long.decode(text);
			}

			if (-2147483648 <= val && val <= 2147483647) {
				return new VarInt(line, col, (int) val);
			} else {
				return new VarLong(line, col, val);
			}
		} catch (Exception e) {
			parserErrors.errors.add(new ErrorHolder(sourceName, line, col, String.format("Literal '%s' is not a valid integer or long", text)));
			return null;
		}
	}

	private VarShort parseShort(int line, int col, String text) {
		try {
			return new VarShort(line, col, Short.decode(text));
		} catch (Exception e) {
			parserErrors.errors.add(new ErrorHolder(sourceName, line, col, String.format("Literal '%s' is not a valid short", text)));
			return null;
		}
	}

	@Override
	public ArrayList<LineHolder> visitStmts(ConcurnasParser.StmtsContext ctx) {
		ArrayList<LineHolder> ret = new ArrayList<LineHolder>();
		for (ConcurnasParser.CsOrssContext sst : ctx.csOrss()) {
			Node got = (Node) sst.accept(this);

			if (got instanceof Expression && !(got instanceof Statement)) {
				got = new DuffAssign((Expression) got);
			}

			Line x = (Line) got;
			if (x != null) {
				ret.add(new LineHolder(x));
			}
		}

		return ret;
	}

	@Override
	public Block visitCode(ConcurnasParser.CodeContext ctx) {
		Block ret = new Block(0, 0);

		for (ConcurnasParser.LineContext lc : ctx.line()) {
			ArrayList<LineHolder> got = (ArrayList<LineHolder>) lc.accept(this);
			ret.addAll(got);
		}

		return ret;
	}

	@Override
	public Expression visitOnchangeEveryShorthand(ConcurnasParser.OnchangeEveryShorthandContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		ArrayList<Node> onchangearges = ctx.onChangeEtcArgs() == null ? new ArrayList<Node>() : (ArrayList<Node>) ctx.onChangeEtcArgs().accept(this);

		Expression rhs = (Expression) ctx.expr_stmt_tuple().accept(this);
		Block blk = new Block(line, col);
		blk.add(new LineHolder(line, col, new DuffAssign(rhs)));
		blk.setShouldBePresevedOnStack(true);

		if (ctx.isEvery != null) {
			return new OnEvery(line, col, onchangearges, blk, new ArrayList<String>());
		} else {
			return new OnChange(line, col, onchangearges, blk, new ArrayList<String>());
		}
	}

	@Override
	public Object visitLonleyExpression(ConcurnasParser.LonleyExpressionContext ctx) {
		Expression expr = (Expression) ctx.expr_stmt_tuple().accept(this);
		return new DuffAssign(ctx.start.getLine(), ctx.start.getCharPositionInLine(), expr);
	}
	
	@Override
	public Thruple<Boolean, Boolean, Boolean> visitTransientAndShared(ConcurnasParser.TransientAndSharedContext ctx) {
		boolean trans = ctx.trans != null;
		boolean shared = ctx.shared != null;
		boolean lazy = ctx.lazy != null;
		
		return new Thruple<Boolean, Boolean, Boolean>(trans, shared, lazy);
	}

	@Override
	public Object visitAssignmentTupleDereflhs(ConcurnasParser.AssignmentTupleDereflhsContext ctx) {
		
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		
		AccessModifier accessModi = (AccessModifier) (ctx.ppp() != null ? ctx.ppp().accept(this) : null);
		
		boolean isTransien;
		boolean isShared;
		boolean isLazy;
		if(null != ctx.transAndShared) {
			Thruple<Boolean, Boolean, Boolean> transAndShared = (Thruple<Boolean, Boolean, Boolean>)ctx.transAndShared.accept(this);
			isTransien = transAndShared.getA();
			isShared = transAndShared.getB();
			isLazy = transAndShared.getC();
		}else {
			isTransien = false;
			isShared = false;
			isLazy = false;
		}

		boolean hasValVar = ctx.valvar != null;
		boolean isFinal = hasValVar && (ctx.valvar.getText().equals("val"));

		String prefix = ctx.prefix == null ? null : ctx.prefix.getText();

		Expression assignee;

		if (ctx.refname != null) {
			assignee = new RefName(line, col, ctx.refname.getText());
		} else {
			assignee = (Expression) ctx.assignee.accept(this);
		}

		Type type = ctx.type() == null ? null : (Type) ctx.type().accept(this);

		Assign ass;
		int refCnt = ctx.refCnt.size();
		if ((accessModi != null || hasValVar || isTransien || isLazy || isShared || prefix != null || type != null) && assignee instanceof RefName && (hasValVar || isTransien || isLazy || isShared || isFinal || type != null || prefix != null || accessModi != null)) {// AssignNew

			if (type == null && refCnt > 0) {
				type = new NamedType(line, col, new ClassDefJava(Object.class));

				for (int na = 0; na < refCnt; na++) {
					type = new NamedType(line, col, type);
				}
			}

			ass = new AssignNew(accessModi, line, col, isFinal, false, ((RefName) assignee).name, prefix, type, null, null);
			((AssignNew) ass).isReallyNew = hasValVar;
			ass.refCnt = refCnt;
		} else {// AssignExisting
			ass = new AssignExisting(line, col, assignee, null, null);
			ass.refCnt = refCnt;
		}

		if (ctx.annotations() != null) {
			Annotations annots = (Annotations) ctx.annotations().accept(this);
			ass.setAnnotations(annots);
		}

		ass.isOverride = ctx.override != null;

		ass.isTransient = isTransien;
		ass.isShared = isShared;
		ass.isLazy = isLazy;

		return ass;
	}

	@Override
	public Object visitAssignmentTupleDereflhsOrNothing(ConcurnasParser.AssignmentTupleDereflhsOrNothingContext ctx) {
		return ctx.assignmentTupleDereflhs() != null?ctx.assignmentTupleDereflhs().accept(this):null;
	}
	
	@Override
	public Object visitAssignment(ConcurnasParser.AssignmentContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Assign ass;
		AssignWithRHSExpression rhsassissgment = null;
		Expression assignment;
		
		if(!ctx.assignmentTupleDereflhsOrNothing().isEmpty()) {
			ArrayList<Assign> lhss = new ArrayList<Assign>();
			ctx.assignmentTupleDereflhsOrNothing().forEach(a -> lhss.add((Assign)a.accept(this)));
			
			AssignStyleEnum assignStyle;
			if (ctx.onchangeEveryShorthand() != null) {
				assignStyle = AssignStyleEnum.EQUALS;
				assignment = (Expression) ctx.onchangeEveryShorthand().accept(this);
			} else {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//

				if (ctx.rhsAssignment != null) {
					rhsassissgment = (AssignWithRHSExpression) ctx.rhsAssignment.accept(this);
					assignment = rhsassissgment.getRHSExpression();
				} else {
					assignment = ctx.rhsExpr == null ? null : (Expression) ctx.rhsExpr.accept(this);//
				}
			}
			
			ass = new AssignTupleDeref(line, col, lhss, assignStyle, assignment);
		}else {

			if (ctx.lonleyannotation != null) {
				return new DuffAssign(line, col, (Annotation) ctx.lonleyannotation.accept(this));
			}

			Pair<AccessModifier, Boolean> accessModifierAndInject = (Pair<AccessModifier, Boolean>)(ctx.ppp() != null ? ctx.ppp().accept(this) : null);
			
			AccessModifier accessModi = accessModifierAndInject==null?null:accessModifierAndInject.getA();
			
			boolean isTransien;
			boolean isShared;
			boolean isLazy;
			if(null != ctx.transAndShared) {
				Thruple<Boolean, Boolean, Boolean> transAndShared = (Thruple<Boolean, Boolean, Boolean>)ctx.transAndShared.accept(this);
				isTransien = transAndShared.getA();
				isShared = transAndShared.getB();
				isLazy = transAndShared.getC();
			}else {
				isTransien = false;
				isShared = false;
				isLazy = false;
			}
			
			boolean hasValVar = ctx.valvar != null;
			boolean isFinal = hasValVar && (ctx.valvar.getText().equals("val"));

			String prefix = ctx.prefix == null ? null : ctx.prefix.getText();

			Expression assignee;

			if (ctx.refname != null) {
				assignee = new RefName(line, col, ctx.refname.getText());
			} else {
				assignee = (Expression) ctx.assignee.accept(this);
			}

			Type type = null;
			if(null != ctx.typeNoNTTuple()) {
				type = (Type) ctx.typeNoNTTuple().accept(this);
			}
			
			AssignStyleEnum assignStyle;
			if (ctx.onchangeEveryShorthand() != null) {
				assignStyle = AssignStyleEnum.EQUALS;
				assignment = (Expression) ctx.onchangeEveryShorthand().accept(this);
			} else if (ctx.rhsAnnotShurtcut != null) {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//
				assignment = (Expression) ctx.rhsAnnotShurtcut.accept(this);
			} else {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//

				if (ctx.rhsAssignment != null) {
					rhsassissgment = (AssignWithRHSExpression) ctx.rhsAssignment.accept(this);
					assignment = rhsassissgment.getRHSExpression();
				} else {
					assignment = ctx.rhsExpr == null ? null : (Expression) ctx.rhsExpr.accept(this);//
				}
			}

			int refCnt = ctx.refCnt.size();
			if ((accessModi != null || hasValVar || isTransien || isLazy || isShared || prefix != null || type != null || assignStyle == AssignStyleEnum.EQUALS) && assignee instanceof RefName && (hasValVar || isFinal || isTransien || isShared || type != null || prefix != null || accessModi != null)) {// AssignNew

				if (type == null && assignment == null && refCnt > 0) {
					type = new NamedType(line, col, new ClassDefJava(Object.class));

					for (int na = 0; na < refCnt; na++) {
						type = new NamedType(line, col, type);
					}
				}

				ass = new AssignNew(accessModi, line, col, isFinal, false, ((RefName) assignee).name, prefix, type, assignStyle, assignment);
				((AssignNew) ass).isReallyNew = hasValVar;
				ass.refCnt = refCnt;
			} else {// AssignExisting
				if (assignStyle == null && assignment == null) {
					ass = new DuffAssign(line, col, assignee);
					return ass;
				} else {
					ass = new AssignExisting(line, col, assignee, assignStyle, assignment);
					ass.refCnt = refCnt;
				}
			}

			if (ctx.annotations() != null) {
				Annotations annots = (Annotations) ctx.annotations().accept(this);
				ass.setAnnotations(annots);
			}

			ass.isTransient = isTransien;
			ass.isShared = isShared;
			ass.isLazy = isLazy;
			if (null != ctx.gpuVarQualifier()) {
				ass.gpuVarQualifier = (GPUVarQualifier) ctx.gpuVarQualifier().accept(this);
			}
			
			ass.isInjected = accessModifierAndInject==null?false:accessModifierAndInject.getB();
		}
		
		ass.isOverride = ctx.override != null;
		
		if (rhsassissgment != null) {
			AssignMulti newOne = new AssignMulti(line, col, assignment);
			// ass.setRHSExpression(null);

			newOne.assignments.add(ass);

			if (rhsassissgment instanceof Assign) {
				Assign asAss = (Assign) rhsassissgment;
				// asAss.setRHSExpression(null);
				newOne.assignments.add(asAss);
			} else {
				AssignMulti multi = (AssignMulti) rhsassissgment;
				// multi.assignments.forEach(a -> a.setRHSExpression(null));
				newOne.assignments.addAll(multi.assignments);
			}
			
			return newOne;
		}
		
		return ass;
	}


	@Override
	public Object visitAssignmentForcedRHS(ConcurnasParser.AssignmentForcedRHSContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Assign ass;
		AssignWithRHSExpression rhsassissgment = null;
		Expression assignment;
		
		if(!ctx.assignmentTupleDereflhsOrNothing().isEmpty()) {
			ArrayList<Assign> lhss = new ArrayList<Assign>();
			ctx.assignmentTupleDereflhsOrNothing().forEach(a -> lhss.add((Assign)a.accept(this)));
			
			AssignStyleEnum assignStyle;
			if (ctx.onchangeEveryShorthand() != null) {
				assignStyle = AssignStyleEnum.EQUALS;
				assignment = (Expression) ctx.onchangeEveryShorthand().accept(this);
			} else {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//

				if (ctx.rhsAssignment != null) {
					rhsassissgment = (AssignWithRHSExpression) ctx.rhsAssignment.accept(this);
					assignment = rhsassissgment.getRHSExpression();
				} else {
					assignment = ctx.rhsExpr == null ? null : (Expression) ctx.rhsExpr.accept(this);//
				}
			}
			
			ass = new AssignTupleDeref(line, col, lhss, assignStyle, assignment);
		}else {

			if (ctx.lonleyannotation != null) {
				return new DuffAssign(line, col, (Annotation) ctx.lonleyannotation.accept(this));
			}

			Pair<AccessModifier, Boolean> accessModifierAndInject = (Pair<AccessModifier, Boolean>)(ctx.ppp() != null ? ctx.ppp().accept(this) : null);
			AccessModifier accessModi = accessModifierAndInject == null?null:accessModifierAndInject.getA();
			
			boolean isTransien;
			boolean isShared;
			boolean isLazy;
			if(null != ctx.transAndShared) {
				Thruple<Boolean, Boolean, Boolean> transAndShared = (Thruple<Boolean, Boolean, Boolean>)ctx.transAndShared.accept(this);
				isTransien = transAndShared.getA();
				isShared = transAndShared.getB();
				isLazy = transAndShared.getC();
			}else {
				isTransien = false;
				isShared = false;
				isLazy = false;
			}
			
			boolean hasValVar = ctx.valvar != null;
			boolean isFinal = hasValVar && (ctx.valvar.getText().equals("val"));

			String prefix = ctx.prefix == null ? null : ctx.prefix.getText();

			Expression assignee;

			if (ctx.refname != null) {
				assignee = new RefName(line, col, ctx.refname.getText());
			} else {
				assignee = (Expression) ctx.assignee.accept(this);
			}

			Type type = null;
			if(null != ctx.type()) {
				type = (Type) ctx.type().accept(this);
			}
			
			AssignStyleEnum assignStyle;
			if (ctx.onchangeEveryShorthand() != null) {
				assignStyle = AssignStyleEnum.EQUALS;
				assignment = (Expression) ctx.onchangeEveryShorthand().accept(this);
			} else if (ctx.rhsAnnotShurtcut != null) {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//
				assignment = (Expression) ctx.rhsAnnotShurtcut.accept(this);
			} else {
				assignStyle = ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this);//

				if (ctx.rhsAssignment != null) {
					rhsassissgment = (AssignWithRHSExpression) ctx.rhsAssignment.accept(this);
					assignment = rhsassissgment.getRHSExpression();
				} else {
					assignment = ctx.rhsExpr == null ? null : (Expression) ctx.rhsExpr.accept(this);//
				}
			}

			int refCnt = ctx.refCnt.size();
			if ((accessModi != null || isTransien || isLazy || isShared|| hasValVar || prefix != null || type != null || assignStyle == AssignStyleEnum.EQUALS) && assignee instanceof RefName && (hasValVar || isTransien || isShared || isFinal || type != null || prefix != null || accessModi != null)) {// AssignNew

				if (type == null && assignment == null && refCnt > 0) {
					type = new NamedType(line, col, new ClassDefJava(Object.class));

					for (int na = 0; na < refCnt; na++) {
						type = new NamedType(line, col, type);
					}
				}

				ass = new AssignNew(accessModi, line, col, isFinal, false, ((RefName) assignee).name, prefix, type, assignStyle, assignment);
				((AssignNew) ass).isReallyNew = hasValVar;
				ass.refCnt = refCnt;
			} else {// AssignExisting
				if (assignStyle == null && assignment == null) {
					ass = new DuffAssign(line, col, assignee);
					return ass;
				} else {
					ass = new AssignExisting(line, col, assignee, assignStyle, assignment);
					ass.refCnt = refCnt;
				}
			}

			if (ctx.annotations() != null) {
				Annotations annots = (Annotations) ctx.annotations().accept(this);
				ass.setAnnotations(annots);
			}
			
			ass.isInjected = accessModifierAndInject == null?false:accessModifierAndInject.getB();

			ass.isTransient = isTransien;
			ass.isShared = isShared;
			ass.isLazy = isLazy;
			if (null != ctx.gpuVarQualifier()) {
				ass.gpuVarQualifier = (GPUVarQualifier) ctx.gpuVarQualifier().accept(this);
			}
		}
		
		ass.isOverride = ctx.override != null;
		
		if (rhsassissgment != null) {
			AssignMulti newOne = new AssignMulti(line, col, assignment);
			// ass.setRHSExpression(null);

			newOne.assignments.add(ass);

			if (rhsassissgment instanceof Assign) {
				Assign asAss = (Assign) rhsassissgment;
				// asAss.setRHSExpression(null);
				newOne.assignments.add(asAss);
			} else {
				AssignMulti multi = (AssignMulti) rhsassissgment;
				// multi.assignments.forEach(a -> a.setRHSExpression(null));
				newOne.assignments.addAll(multi.assignments);
			}

			return newOne;
		}

		return ass;
		
	}
	
	@Override
	public DeleteStatement visitDelete_stmt(ConcurnasParser.Delete_stmtContext ctx) {
		ArrayList<Expression> exprs = new ArrayList<Expression>(ctx.expr_stmt().size());
		ctx.expr_stmt().forEach(x -> exprs.add((Expression) x.accept(this)));
		return new DeleteStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), exprs);
	}

	@Override
	public Statement visitFlow_stmt(ConcurnasParser.Flow_stmtContext ctx) {
		Expression expr = ctx.expr_stmt_tuple() == null ? null : (Expression) ctx.expr_stmt_tuple().accept(this);
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		switch (ctx.getChild(0).getText()) {
		case "break":
			return expr != null ? new BreakStatement(line, col, expr) : new BreakStatement(line, col);
		case "continue":
			return expr != null ? new ContinueStatement(line, col, expr) : new ContinueStatement(line, col);
		// case "return":
		// return expr != null ? new ReturnStatement(line, col, expr) : new
		// ReturnStatement(line, col);
		// case "throw":
		// return new ThrowStatement(line, col, expr);
		}

		return null;
	}

	@Override
	public ReturnStatement visitReturn_stmt(ConcurnasParser.Return_stmtContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		Expression expr = ctx.expr_stmt_tuple() == null ? null : (Expression) ctx.expr_stmt_tuple().accept(this);
		return expr != null ? new ReturnStatement(line, col, expr) : new ReturnStatement(line, col);
	}

	@Override
	public ThrowStatement visitThrow_stmt(ConcurnasParser.Throw_stmtContext ctx) {
		return new ThrowStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.expr_stmt().accept(this));
	}

	@Override
	public ImportStatement visitImport_stmt_from(ConcurnasParser.Import_stmt_fromContext ctx) {
		boolean normalImport = ctx.using == null;
		if(ctx.star != null) {
			return new ImportStar(ctx.start.getLine(), ctx.start.getCharPositionInLine(), normalImport, (String) ctx.dotted_name().accept(this));
		}else {
			ImportFrom ret = new ImportFrom(ctx.start.getLine(), ctx.start.getCharPositionInLine(), normalImport, (String) ctx.dotted_name().accept(this));

			for (ConcurnasParser.Import_as_nameContext ian : ctx.import_as_name()) {
				ret.add((ImportAsName) ian.accept(this));
			}
			return ret;
		}
	}

	@Override
	public ImportAsName visitImport_as_name(ConcurnasParser.Import_as_nameContext ctx) {

		int namesCnt = ctx.NAME().size();

		if (namesCnt == 2) {
			return new ImportAsName(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME(0).getText(), ctx.NAME(1).getText());
		} else {
			return new ImportAsName(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME(0).getText());
		}
	}

	@Override
	public ImportStatement visitImport_stmt_impot(ConcurnasParser.Import_stmt_impotContext ctx) {
		boolean normalImport = ctx.using == null;
		if(ctx.star != null) {
			return new ImportStar(ctx.start.getLine(), ctx.start.getCharPositionInLine(), normalImport, (String) ctx.dotted_name().accept(this));
		}else {
			ImportImport ret = new ImportImport(ctx.start.getLine(), ctx.start.getCharPositionInLine(), normalImport);
			DottedAsName primdasn = (DottedAsName) ctx.prim.accept(this);
			ret.add(primdasn);

			for (ConcurnasParser.Dotted_as_nameContext dasn : ctx.sec) {
				ret.add(primdasn, (DottedAsName) dasn.accept(this));
			}

			return ret;
		}
	}

	@Override
	public Await visitAwait_stmt(ConcurnasParser.Await_stmtContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		ArrayList<Node> args = (ArrayList<Node>) ctx.onChangeEtcArgs().accept(this);

		Block check;
		if (null != ctx.expr_stmt()) {
			check = new Block(line, col, (Expression) ctx.expr_stmt().accept(this));
		} else if (ctx.block() != null) {
			check = (Block) ctx.block().accept(this);
		} else {
			check = null;
		}

		return new Await(line, col, args, check);
	}

	@Override
	public ArrayList<Node> visitOnChangeEtcArgs(ConcurnasParser.OnChangeEtcArgsContext ctx) {
		ArrayList<Node> ret = new ArrayList<Node>();
		for (ConcurnasParser.OnChangeEtcArgContext cc : ctx.onChangeEtcArg()) {
			ret.add((Node) cc.accept(this));
		}

		return ret;
	}

	@Override
	public Node visitOnChangeEtcArg(ConcurnasParser.OnChangeEtcArgContext ctx) {
		if (ctx.NAME() != null) {
			boolean hasValVar = ctx.valvar != null;
			boolean isFinal = false;
			isFinal = hasValVar && ctx.valvar.getText().equals("val");

			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			Type type = ctx.type() == null ? null : (Type) ctx.type().accept(this);

			AssignNew ass = new AssignNew(null, line, col, isFinal, false, ctx.NAME().getText(), type, AssignStyleEnum.EQUALS, (Expression) ctx.expr_stmt().accept(this));
			((AssignNew) ass).isReallyNew = hasValVar;

			int refCnt = ctx.refCnt.size();
			if (refCnt > 0) {
				ass.refCnt = refCnt;
			}

			return ass;
		} else {
			return (Node) ctx.expr_stmt().accept(this);
		}
	}

	@Override
	public DottedAsName visitDotted_as_name(ConcurnasParser.Dotted_as_nameContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		String dot = (String) ctx.dotted_name().accept(this);

		if (null == ctx.NAME()) {
			return new DottedAsName(line, col, dot);
		} else {
			return new DottedAsName(line, col, dot, ctx.NAME().getText());
		}
	}

	@Override
	public String visitDotted_name(ConcurnasParser.Dotted_nameContext ctx) {
		List<TerminalNode> elements = ctx.NAME();
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < elements.size(); n++) {
			if (n > 0) {
				sb.append(".");
			}
			sb.append(elements.get(n).getText());
		}

		return sb.toString();
	}

	@Override
	public AssertStatement visitAssert_stmt(ConcurnasParser.Assert_stmtContext ctx) {
		AssertStatement ret = new AssertStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.e.accept(this));

		if (null != ctx.s) {
			ret.message = (VarString) ctx.s.accept(this);
		}

		return ret;
	}

	@Override
	public Type visitPrimitiveType(ConcurnasParser.PrimitiveTypeContext ctx) {
		String txt = ctx.getText();

		if (txt.equals("lambda")) {
			return (Type) ScopeAndTypeChecker.const_lambda_nt.copy();
		}

		return new PrimativeType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), PrimativeTypeEnum.nameToEnum.get(txt));
	}

	@Override
	public AssignStyleEnum visitAssignStyle(ConcurnasParser.AssignStyleContext ctx) {
		// String item = ctx.getText();
		return AssignStyleEnum.assignStyleToEnum.get(ctx.getText());
	}

	public Expression visitRefName(ConcurnasParser.RefNameContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		// gentypes?
		return new RefName(line, col, ctx.NAME().getText());
	}

	@Override
	public Expression visitAtom(ConcurnasParser.AtomContext ctx) {
		// todo: more
		return (Expression) visitChildren(ctx);
	}

	@Override
	public Expression visitIntNode(ConcurnasParser.IntNodeContext ctx) {
		return (Expression) parseLongOrInt(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
	}

	@Override
	public Expression visitLongNode(ConcurnasParser.LongNodeContext ctx) {
		String llongs = ctx.getText();
		return (Expression) new VarLong(ctx.start.getLine(), ctx.start.getCharPositionInLine(), Long.parseLong(llongs.substring(0, llongs.length() - 1)));
	}

	@Override
	public Expression visitShortNode(ConcurnasParser.ShortNodeContext ctx) {
		String llongs = ctx.getText();
		return (Expression) parseShort(ctx.start.getLine(), ctx.start.getCharPositionInLine(), llongs.substring(0, llongs.length() - 1));
	}

	@Override
	public Expression visitFloatNode(ConcurnasParser.FloatNodeContext ctx) {
		String llongs = ctx.getText();
		return (Expression) new VarFloat(ctx.start.getLine(), ctx.start.getCharPositionInLine(), Float.parseFloat(llongs.substring(0, llongs.length() - 1)));
	}

	@Override
	public Expression visitDoubleNode(ConcurnasParser.DoubleNodeContext ctx) {
		String llongs = ctx.getText();
		return (Expression) new VarDouble(ctx.start.getLine(), ctx.start.getCharPositionInLine(), Double.parseDouble(llongs));
	}

	/*
	 * @Override public Expression visitCharNode(ConcurnasParser.CharNodeContext
	 * ctx) { String ctext = ctx.getText();
	 * 
	 * ctext = StringUtils.unescapeJavaString(ctext);
	 * 
	 * return new VarChar(ctx.start.getLine(), ctx.start.getCharPositionInLine(),
	 * ctext.substring(2, ctext.length() - 1)); }
	 */

	@Override
	public Expression visitStringNode(ConcurnasParser.StringNodeContext ctx) {
		String ctext = ctx.getText();

		ctext = StringUtils.unescapeJavaString(ctext);
		String subx = ctext.substring(1, ctext.length() - 1);

		if (ctx.isQuote != null && ctext.length() == 3) {// it's a 'c' <- this is a char
			return new VarChar(ctx.start.getLine(), ctx.start.getCharPositionInLine(), subx);
		} else {
			return new VarString(ctx.start.getLine(), ctx.start.getCharPositionInLine(), subx);
		}
	}
	
	
	@Override
	public Expression visitLangExtNode(ConcurnasParser.LangExtNodeContext ctx) {
		String bbody = ctx.body.getText();
		return new LangExt(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.name.getText(), bbody.substring(2, bbody.length()-2));
	}

	@Override
	public VarRegexPattern visitRegexStringNode(ConcurnasParser.RegexStringNodeContext ctx) {
		String ctext = ctx.getText();
		return new VarRegexPattern(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctext.substring(2, ctext.length() - 1));
	}

	@Override
	public RefBoolean visitBooleanNode(ConcurnasParser.BooleanNodeContext ctx) {
		return new RefBoolean(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText().equals("true"));
	}

	@Override
	public AccessModifier visitPppNoInject(ConcurnasParser.PppNoInjectContext ctx) {
		ParseTree whichone = ctx.getChild(0);
		
		if (whichone == ctx.PRIVATE()) {
			return AccessModifier.PRIVATE;
		} else if (whichone == ctx.PUBLIC()) {
			return AccessModifier.PUBLIC;
		} else if (whichone == ctx.PROTECTED()) {
			return AccessModifier.PROTECTED;
		} else if (whichone == ctx.PACKAGE()) {
			return AccessModifier.PACKAGE;
		}
		
		return null;
	}
	@Override
	public Pair<AccessModifier, Boolean> visitPpp(ConcurnasParser.PppContext ctx) {
		boolean inject = ctx.inject != null;
		
		String pp = ctx.pp == null?null:ctx.pp.getText();
		AccessModifier am = AccessModifier.PUBLIC;
		if(null != pp) {
			if (pp.equals("private")) {
				am = AccessModifier.PRIVATE;
			} else if (pp.equals("public")) {
				am = AccessModifier.PUBLIC;
			} else if (pp.equals("protected")) {
				am = AccessModifier.PROTECTED;
			} else if (pp.equals("package")) {
				am = AccessModifier.PACKAGE;
			}
		}
		
		return new Pair<AccessModifier, Boolean>(am, inject);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Pair<String, NamedType>> visitGenericQualiList(ConcurnasParser.GenericQualiListContext ctx) {
		ArrayList<Pair<String, NamedType>> ret = new ArrayList<Pair<String, NamedType>>(2);

		for (ConcurnasParser.NameAndUpperBoundContext tn : ctx.nameAndUpperBound()) {
			ret.add((Pair<String, NamedType>)tn.accept(this));
		}

		return ret;
	}
	
	@Override
	public Pair<String, NamedType> visitNameAndUpperBound(ConcurnasParser.NameAndUpperBoundContext ctx) {
		
		String name = ctx.NAME().getText();
		NamedType nt = null != ctx.namedType()?(NamedType)ctx.namedType().accept(this):null;
		
		boolean nullable = ctx.nullable != null;
		if(nullable) {
			if(null == nt) {
				nt = ScopeAndTypeChecker.const_object.copyTypeSpecific();
			}
			nt.setNullStatus(NullStatus.NULLABLE);
		}
		
		return new Pair<String, NamedType>(name, nt);
	}
	

	@Override
	public List<String> visitTypedefArgs(ConcurnasParser.TypedefArgsContext ctx) {
		return ctx.NAME().stream().map(a -> a.getText()).collect(Collectors.toList());
		
	}

	@Override
	public TypedefStatement visitTypedef_stmt(ConcurnasParser.Typedef_stmtContext ctx) {
		AccessModifier accessModifier = (AccessModifier)(ctx.pppNoInject() != null ? ctx.pppNoInject().accept(this) : null);
		
		List<String> typedefargs = ctx.typedefArgs() == null ? new ArrayList<String>(0) : (List<String>) ctx.typedefArgs().accept(this);

		Type type = (Type) ctx.type().accept(this);

		return new TypedefStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), accessModifier, ctx.NAME().getText(), type, typedefargs);
	}

	@Override
	public GrandLogicalOperatorEnum visitEqualityOperator(ConcurnasParser.EqualityOperatorContext ctx) {
		return GrandLogicalOperatorEnum.symToEnum.get(ctx.getText());
	}

	@Override
	public Statement visitCompound_stmt(ConcurnasParser.Compound_stmtContext ctx) {
		int strtChildIdx = ctx.children.size() - 1;// ctx.annotations() != null ? 1 : 0;
		/*
		 * if(ctx.NEWLINE() != null){ strtChildIdx++; }
		 */

		Statement ret = (Statement) ctx.getChild(strtChildIdx).accept(this);

		if (ctx.annotations() != null) {
			((HasAnnotations) ret).setAnnotations((Annotations) ctx.annotations().accept(this));
		}

		return ret;
	}
	
	@Override
	public Block visitSingle_line_block(ConcurnasParser.Single_line_blockContext ctx) {
		Block ret = new Block(ctx.start.getLine(), ctx.start.getCharPositionInLine());
		
		// ret.addAll((List<LineHolder>) ctx.line().accept(this));
		ctx.single_line_element().forEach(a -> ret.add((LineHolder) a.accept(this)));
		
		return ret;
	}


	/*
	 * @Override public Object
	 * visitExpr_list_shortcut(ConcurnasParser.Expr_list_shortcutContext ctx) {
	 * Object g = ctx.expr_stmt_(0).accept(this);
	 * 
	 * String rname = ctx.refName(0).getText(); String rname2 =
	 * ctx.refName(0).getText();
	 * 
	 * return visitChildren(ctx); }
	 */
	@Override
	public LineHolder visitSingle_line_element(ConcurnasParser.Single_line_elementContext ctx) {

		if (null != ctx.nop) {
			return new LineHolder(new NOP());
		} else if (null != ctx.simple_stmt()) {
			Node got = (Node) ctx.simple_stmt().accept(this);
			if (got instanceof Expression) {
				got = new DuffAssign((Expression) got);
			}

			return new LineHolder((Statement) got);
		} else if (null != ctx.comppound_str_concat()) {
			Statement single = (Statement) ctx.comppound_str_concat().accept(this);
			return new LineHolder(single);
		} else {
			Statement single = (Statement) ctx.compound_stmt().accept(this);
			return new LineHolder(single);
		}

	}

	@Override
	public List<LineHolder> visitLine(ConcurnasParser.LineContext ctx) {
		List<LineHolder> ret = new ArrayList<LineHolder>();

		if (null != ctx.nop) {
			ret.add(new LineHolder(new NOP()));
		} else if (null != ctx.stmts()) {
			ret.addAll((List<LineHolder>) ctx.stmts().accept(this));
		} else if (ctx.nls != null) {
			// skip
		}
		/*
		 * else { Statement single = (Statement) ctx.compound_stmt().accept(this);
		 * ret.add(new LineHolder(single)); }
		 */

		return ret;
	}

	@Override
	public Block visitBlock_(ConcurnasParser.Block_Context ctx) {
		Block ret = new Block(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		ctx.line().forEach(a -> ret.addAll((List<LineHolder>) a.accept(this)));

		return ret;
	}

	@Override
	public Object visitFuncDefName(ConcurnasParser.FuncDefNameContext ctx) {
		if (ctx.NAME() != null) {
			return ctx.NAME().getText();
		} else {
			String txt = ctx.getText();
			switch (txt) {
			case "=":
				return "assign";
			case "+":
				return "plus";
			case "-":
				return "minus";
			case "*":
				return "mul";
			case "/":
				return "div";
			case "**":
				return "pow";
			case "++":
				return "inc";
			case "--":
				return "dec";
			case "<<":
				return "leftShift";
			case ">>":
				return "rightShift";
			case ">>>":
				return "rightShiftU";

			case "comp":
				return "comp";
			case "band":
				return "band";
			case "bor":
				return "bor";
			case "bxor":
				return "bxor";

			case "+=":
				return "plusAssign";
			case "-=":
				return "minusAssign";
			case "*=":
				return "mulAssign";
			case "/=":
				return "divAssign";
			case "**=":
				return "powAssign";
			case "++=":
				return "incAssign";
			case "--=":
				return "decAssign";
			case "<<=":
				return "leftShiftAssign";
			case ">>=":
				return "rightShiftAssign";
			case ">>>=":
				return "rightShiftUAssign";

			case "comp=":
				return "compAssign";
			case "band=":
				return "bandAssign";
			case "bor=":
				return "borAssign";
			case "bxor=":
				return "bxorAssign";

			default:
				return txt;
			}
		}
	}

	@Override
	public FuncDefI visitFuncdef(ConcurnasParser.FuncdefContext ctx) {
		//AccessModifier accessModi = (AccessModifier) (ctx.ppp() != null ? ctx.ppp().accept(this) : null);
		Pair<AccessModifier, Boolean> accessModifierAndInject = (Pair<AccessModifier, Boolean>)(ctx.ppp() != null ? ctx.ppp().accept(this) : null);
		AccessModifier accessModi = accessModifierAndInject == null?null:accessModifierAndInject.getA();
		
		String funcName = ctx.funcDefName() == null ? null : (String) ctx.funcDefName().accept(this);
		boolean override = ctx.override != null;
		boolean isFinal = ctx.DOT() != null;

		ArrayList<Pair<String, NamedType>> methodGenricList = null != ctx.genericQualiList() ? (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this) : new ArrayList<Pair<String, NamedType>>();

		Type retType = ctx.retTypeIncVoid() == null ? null : (Type) ctx.retTypeIncVoid().accept(this);
		boolean aabstract = false;
		Block body;
		if (ctx.block() != null) {
			body = (Block) ctx.block().accept(this);
		} else if (ctx.single_line_block() != null) {
			body = (Block) ctx.single_line_block().accept(this);
		} else {
			aabstract = true;
			body = null;
		}

		FuncParams params = ctx.funcParams() != null ? (FuncParams) ctx.funcParams().accept(this) : new FuncParams(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		FuncDefI ret;

		String gpuitem = ctx.gpuitem == null ? null : ctx.gpuitem.getText();

		if (null != funcName) {
			FuncDef fd = new FuncDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), null, accessModi, funcName, params, body, retType, override, aabstract, isFinal, methodGenricList);

			if (!ctx.extFuncOn().isEmpty()) {
				List<ConcurnasParser.ExtFuncOnContext> extons = ctx.extFuncOn();
				ConcurnasParser.ExtFuncOnContext first = extons.get(0);

				if (extons.size() == 1) {
					fd.extFunOn = (Type) first.accept(this);
				} else {
					ArrayList<Type> multitype = new ArrayList<Type>(extons.size());
					extons.forEach(btc -> multitype.add((Type) btc.accept(this)));
					fd.extFunOn = new MultiType(first.start.getLine(), first.start.getCharPositionInLine(), multitype);
				}
			}
			ret = fd;

			if (ctx.kerneldim != null) {
				fd.kernelDim = (Expression) ctx.kerneldim.accept(this);
			}

		} else {
			ret = new LambdaDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), null, params, body, retType, methodGenricList);
		}

		if (gpuitem != null) {
			ret.isGPUKernalOrFunction = GPUFuncVariant.valueOf(gpuitem);
		}
		
		ret.isInjected = accessModifierAndInject==null?false:accessModifierAndInject.getB();
		return ret;
	}

	@Override
	public Type visitExtFuncOn(ConcurnasParser.ExtFuncOnContext ctx) {
		return (Type) ctx.type().accept(this);

		/*
		 * if(ctx.extFunOn != null){ return (NamedType)ctx.extFunOn.accept(this); } else
		 * { //if(ctx.extFunOnPrim != null){ return
		 * (PrimativeType)ctx.extFunOnPrim.accept(this); }
		 */
	}

	@Override
	public LambdaDef visitLambdadef(ConcurnasParser.LambdadefContext ctx) {
		ArrayList<Pair<String, NamedType>> methodGenricList = null != ctx.genericQualiList() ? (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this) : new ArrayList<Pair<String, NamedType>>();
		
		Type retType = ctx.retTypeIncVoid() == null ? null : (Type) ctx.retTypeIncVoid().accept(this);
		Block body;
		if (ctx.block() != null) {
			body = (Block) ctx.block().accept(this);
		} else if (ctx.single_line_block() != null) {
			body = (Block) ctx.single_line_block().accept(this);
		} else {
			body = null;
		}
		
		FuncParams params = ctx.funcParams() != null ? (FuncParams) ctx.funcParams().accept(this) : new FuncParams(ctx.start.getLine(), ctx.start.getCharPositionInLine());
		
		return new LambdaDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), null, params, body, retType, methodGenricList);
	}
	
	@Override
	public LambdaDef visitLambdadefOneLine(ConcurnasParser.LambdadefOneLineContext ctx) {
		ArrayList<Pair<String, NamedType>> methodGenricList = null != ctx.genericQualiList() ? (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this) : new ArrayList<Pair<String, NamedType>>();

		Type retType = ctx.retTypeIncVoid() == null ? null : (Type) ctx.retTypeIncVoid().accept(this);
		Block body = (Block) ctx.single_line_block().accept(this);

		FuncParams params = ctx.funcParams() != null ? (FuncParams) ctx.funcParams().accept(this) : new FuncParams(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		return new LambdaDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), null, params, body, retType, methodGenricList);
	}

	@Override
	public FuncParams visitFuncParams(ConcurnasParser.FuncParamsContext ctx) {
		FuncParams ret = new FuncParams(ctx.start.getLine(), ctx.start.getCharPositionInLine());
		ctx.funcParam().forEach(a -> ret.add((FuncParam) a.accept(this)));
		return ret;
	}

	@Override
	public FuncParam visitFuncParam(ConcurnasParser.FuncParamContext ctx) {
		boolean isFinal = ctx.isFinal != null;

		Type type = ctx.type() != null ? (Type) ctx.type().accept(this) : null;
		// Type type = (Type) ctx.type().accept(this);

		FuncParam ret = new FuncParam(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME() == null ? null : ctx.NAME().getText(), type, isFinal);

		ret.annotations = ctx.annotations() == null ? null : (Annotations) ctx.annotations().accept(this);
		ret.isVararg = ctx.isvararg != null;
		if (ctx.expr_stmt() != null) {
			ret.defaultValue = (Expression) ctx.expr_stmt().accept(this);
		}

		if (null != ctx.gpuVarQualifier()) {
			ret.gpuVarQualifier = (GPUVarQualifier) ctx.gpuVarQualifier().accept(this);
		}

		if (null != ctx.gpuInOutFuncParamModifier()) {
			ret.gpuInOutFuncParamModifier = (GPUInOutFuncParamModifier) ctx.gpuInOutFuncParamModifier().accept(this);
		}
		
		if(ctx.sharedOrLazy() != null) {
			Pair<Boolean, Boolean> sl = (Pair<Boolean, Boolean>)ctx.sharedOrLazy().accept(this);
			
			ret.isShared = sl.getA();
			ret.isLazy = sl.getB();
		}

		return ret;
	}

	@Override
	public Pair<Boolean, Boolean> visitSharedOrLazy(ConcurnasParser.SharedOrLazyContext ctx) {
		return new Pair<Boolean, Boolean>(ctx.shared!=null, ctx.lazy!=null);
	}
	
	
	@Override
	public GPUInOutFuncParamModifier visitGpuInOutFuncParamModifier(ConcurnasParser.GpuInOutFuncParamModifierContext ctx) {
		return GPUInOutFuncParamModifier.valueOf(ctx.getText());
	}

	@Override
	public GPUVarQualifier visitGpuVarQualifier(ConcurnasParser.GpuVarQualifierContext ctx) {
		String ss = ctx.getText();
		return GPUVarQualifier.valueOf(ss.toUpperCase());

	}

	private ArrayList<Fourple<RefOrArryEnum, Object, Integer, Integer>> getMutators(List<?> mutators) {
		ArrayList<Fourple<RefOrArryEnum, Object, Integer, Integer>> ret = new ArrayList<Fourple<RefOrArryEnum, Object, Integer, Integer>>(mutators.size());
		mutators.forEach(a -> ret.add( (Fourple<RefOrArryEnum, Object, Integer, Integer>)((ParserRuleContext)a).accept(this)  ));
		return ret;
	}
	
	//private Type applyMutators(Type ret, List<ConcurnasParser.TrefOrArrayRefContext> mutators) {
	private Type applyMutators(Type ret, List<Fourple<RefOrArryEnum, Object, Integer, Integer>> mutators) {
		if(mutators == null) {
			return ret;
		}
		
		//for (ConcurnasParser.TrefOrArrayRefContext mutator : mutators) {
			//Fourple<RefOrArryEnum, Object, Integer, Integer> muta = (Fourple<RefOrArryEnum, Object, Integer, Integer>) mutator.accept(this);

		for (Fourple<RefOrArryEnum, Object, Integer, Integer> muta : mutators) {
			RefOrArryEnum what = muta.getA();

			if (what == RefOrArryEnum.ARRAY) {
				Expression specLevels = (Expression) muta.getB();

				if (specLevels instanceof VarLong) {
					parserErrors.errors.add(new ErrorHolder(sourceName, muta.getC(), muta.getD(), String.format("Only integers can be used as array size arguments")));
					return ret;
				}
				ret = (Type) ret.copy();
				ret.setArrayLevels(((VarInt) specLevels).inter);
				if(ret.getNullStatus() == NullStatus.NULLABLE) {
					ret.setNullStatus(NullStatus.NONNULL);
					ret.setNullStatusAtArrayLevel(NullStatus.NULLABLE);
				}
			} else if (what == RefOrArryEnum.REF) {
				ret = new NamedType(muta.getC(), muta.getD(), ret);
			} else if (what == RefOrArryEnum.NAMED_REF) {
				ret = new NamedType(muta.getC(), muta.getD(), (String) muta.getB(), ret);
			}else if(what == RefOrArryEnum.NULLABLE) {
				if(ret.getNullStatus() == NullStatus.NULLABLE && !ret.hasArrayLevels()) {
					parserErrors.errors.add(new ErrorHolder(sourceName, muta.getC(), muta.getD(), "? may not be used consecutively"));
				}
				if(ret instanceof PrimativeType && !ret.hasArrayLevels()) {
					//parserErrors.errors.add(new ErrorHolder(sourceName, muta.getC(), muta.getD(), "primative types may not be nullable"));
				}
				
				/*if(ret.hasArrayLevels()) {
					//add nullable array level
					ret.setNullStatusAtArrayLevel(NullStatus.NULLABLE);
				}else {*/
					ret.setNullStatus(NullStatus.NULLABLE);
				//}
			}
		}
		return ret;
	}

	private enum RefOrArryEnum {
		REF, NAMED_REF, ARRAY, NULLABLE;
	}

	@Override
	public Fourple<RefOrArryEnum, Object, Integer, Integer> visitTrefOrArrayRef(ConcurnasParser.TrefOrArrayRefContext ctx) {
		if (ctx.hasAr != null) {
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.ARRAY, ctx.arLevels == null ? new VarInt(ctx.start.getLine(), ctx.start.getCharPositionInLine(), 1) : ctx.arLevels.accept(this), ctx.start.getLine(), ctx.start.getCharPositionInLine());
		} else if (!ctx.hasArAlt.isEmpty()) {
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.ARRAY, new VarInt(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.hasArAlt.size()), ctx.start.getLine(), ctx.start.getCharPositionInLine());
		} else if(ctx.refOrNullable() != null) {
			return (Fourple<RefOrArryEnum, Object, Integer, Integer>)ctx.refOrNullable().accept(this) ;
		}
		
		else {
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.REF, null, ctx.start.getLine(), ctx.start.getCharPositionInLine());
		}
	}
	
	@Override
	public Fourple<RefOrArryEnum, Object, Integer, Integer> visitRefOrNullable(ConcurnasParser.RefOrNullableContext ctx) {
		if (ctx.dotted_name() != null) {
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.NAMED_REF, ctx.dotted_name().accept(this), ctx.start.getLine(), ctx.start.getCharPositionInLine());
		} else if(ctx.nullable != null){
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.NULLABLE, null, ctx.start.getLine(), ctx.start.getCharPositionInLine());
		} else if(ctx.nullableErr != null){
			parserErrors.errors.add(new ErrorHolder(sourceName, ctx.start.getLine(), ctx.start.getCharPositionInLine(), "? may not be used consecutively"));
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.NULLABLE, null, ctx.start.getLine(), ctx.start.getCharPositionInLine());
		}else {
			return new Fourple<RefOrArryEnum, Object, Integer, Integer>(RefOrArryEnum.REF, null, ctx.start.getLine(), ctx.start.getCharPositionInLine());
		}
	}
	

	@Override
	public FuncType visitFuncType(ConcurnasParser.FuncTypeContext ctx) {
		return (FuncType) ctx.funcType_().accept(this);
	}

	@Override
	public FuncType visitFuncType_(ConcurnasParser.FuncType_Context ctx) {
		ArrayList<Type> inputs = new ArrayList<Type>();

		ctx.type().forEach(a -> inputs.add((Type) a.accept(this)));

		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Type retType = (Type) ctx.retTypeIncVoid().accept(this);

		
		ArrayList<Type> inputsB = new ArrayList<Type>(inputs.size());
		for(Type tt: inputs) {
			inputsB.add(TypeCheckUtils.boxTypeIfPrimative(tt, false));
		}
		
		FuncType ret = new FuncType(line, col, inputsB, TypeCheckUtils.boxTypeIfPrimative(retType, false, false));

		if (ctx.constr != null) {
			ret.isClassRefType = true;
		}

		if (null != ctx.genericQualiList()) {
			ArrayList<Pair<String, NamedType>> gens = (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this);

			ArrayList<GenericType> localGens = new ArrayList<GenericType>(gens.size());

			for (Pair<String, NamedType> gg : gens) {
				GenericType gh = new GenericType(line, col, gg.getA(), 0);
				NamedType nt = gg.getB();
				if(null != nt) {
					gh.upperBound = nt;
					gh.setNullStatus(nt.getNullStatus());
				}
				
				localGens.add(gh);
			}

			ret.setLocalGenerics(localGens);
		}

		return ret;
	}

	@Override
	public ClassDef visitClassdef(ConcurnasParser.ClassdefContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Pair<AccessModifier, Boolean> accessModifierAndInject = (Pair<AccessModifier, Boolean>) (ctx.ppp() != null ? ctx.ppp().accept(this) : null);
		AccessModifier accessModifier = accessModifierAndInject == null?null:accessModifierAndInject.getA();
		
		boolean isActor = ctx.isactor != null;

		@SuppressWarnings("unchecked")
		ArrayList<Pair<String, NamedType>> classGenricList = ctx.genericQualiList() == null ? new ArrayList<Pair<String, NamedType>>() : (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this);

		ClassDefArgs classDefArgs = ctx.classdefArgs() == null ? null : (ClassDefArgs) ctx.classdefArgs().accept(this);

		String superclass = ctx.superCls == null ? null : (String) ctx.superCls.accept(this);

		String className = ctx.className.getText();

		boolean istypedActor = ctx.istypedActor != null;

		NamedType typedActorOn = ctx.typedActorOn == null ? null : (NamedType) ctx.typedActorOn.accept(this);

		String isFinalDefined = null;
		boolean isAbstract = false;
		if (ctx.aoc != null) {
			String aocText = ctx.aoc.getText();
			switch (aocText) {
			case "abstract":
				isAbstract = true;
				break;
			default:
				isFinalDefined = aocText;
				break;
			}
		}

		
		Block classBlock = ctx.block() == null ? new Block(line, col) : (Block) ctx.block().accept(this);

		ArrayList<Expression> acteeClassExpressions = ctx.typeActeeExprList == null ? new ArrayList<Expression>() : (ArrayList<Expression>) ctx.typeActeeExprList.accept(this);

		ArrayList<Expression> superClassExpressions = ctx.extExpressions == null ? new ArrayList<Expression>() : (ArrayList<Expression>) ctx.extExpressions.accept(this);

		ArrayList<Type> superClassGenricList = new ArrayList<Type>();
		ctx.superGenType.forEach(a -> superClassGenricList.add((Type) a.accept(this)));

		ArrayList<ImpliInstance> impls = new ArrayList<ImpliInstance>();
		if (!ctx.implInstance().isEmpty()) {
			ctx.implInstance().forEach(a -> impls.add((ImpliInstance) a.accept(this)));
		}

		boolean isTrait = ctx.istrait != null;
		boolean isTransient = ctx.trans != null;
		boolean isShared = ctx.shared != null;
		
		ClassDef cd = new ClassDef(line, col, accessModifier, false, isActor, className, classGenricList, classDefArgs, superclass, superClassGenricList, superClassExpressions, impls, classBlock, isAbstract, isFinalDefined, typedActorOn, istypedActor, acteeClassExpressions, isTransient, isShared, isTrait);
		cd.injectClassDefArgsConstructor = accessModifierAndInject==null?false:accessModifierAndInject.getB();
		
		return cd;
	}

	@Override
	public ImpliInstance visitImplInstance(ConcurnasParser.ImplInstanceContext ctx) {

		ArrayList<Type> interfaceGenricList = new ArrayList<Type>();
		ctx.implType.forEach(a -> interfaceGenricList.add((Type) a.accept(this)));

		String ifaceName = ctx.impli == null ? null : (String) ctx.impli.accept(this);

		return new ImpliInstance(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ifaceName, interfaceGenricList);
	}

	@Override
	public LocalClassDef visitLocalclassdef(ConcurnasParser.LocalclassdefContext ctx) {

		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		boolean isActor = ctx.isactor != null;

		ArrayList<Pair<String, NamedType>> classGenricList = ctx.genericQualiList() == null ? new ArrayList<Pair<String, NamedType>>() : (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this);

		ClassDefArgs classDefArgs = ctx.classdefArgs() == null ? null : (ClassDefArgs) ctx.classdefArgs().accept(this);

		String superclass = ctx.superCls == null ? null : (String) ctx.superCls.accept(this);

		boolean istypedActor = ctx.istypedActor != null;

		NamedType typedActorOn = ctx.typedActorOn == null ? null : (NamedType) ctx.typedActorOn.accept(this);

		String isFinalDefined = null;
		boolean isAbstract = false;

		Block classBlock = ctx.block() == null ? new Block(line, col) : (Block) ctx.block().accept(this);

		ArrayList<Expression> acteeClassExpressions = ctx.typeActeeExprList == null ? new ArrayList<Expression>() : (ArrayList<Expression>) ctx.typeActeeExprList.accept(this);

		ArrayList<Expression> superClassExpressions = ctx.extExpressions == null ? new ArrayList<Expression>() : (ArrayList<Expression>) ctx.extExpressions.accept(this);

		ArrayList<Type> superClassGenricList = new ArrayList<Type>();

		ctx.type().forEach(a -> superClassGenricList.add((Type) a.accept(this)));

		ArrayList<ImpliInstance> impls = new ArrayList<ImpliInstance>();
		if (!ctx.implInstance().isEmpty()) {
			ctx.implInstance().forEach(a -> impls.add((ImpliInstance) a.accept(this)));
		}

		boolean isTransient = ctx.trans != null;
		boolean isShared = ctx.shared != null;
		
		ClassDef cd = new ClassDef(line, col, null, false, isActor, null, classGenricList, classDefArgs, superclass, superClassGenricList, superClassExpressions, impls, classBlock, isAbstract, isFinalDefined, typedActorOn, istypedActor, acteeClassExpressions, isTransient, isShared, false);

		cd.isLocalClassDef = true;

		return new LocalClassDef(line, col, cd);
	}
	
	@Override
	public Block visitAnonclassdef(ConcurnasParser.AnonclassdefContext ctx) {

		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		boolean isActor = ctx.isactor != null;
		
		String superclass = ctx.superCls == null ? null : (String) ctx.superCls.accept(this);

		String isFinalDefined = null;
		boolean isAbstract = false;

		Block classBlock = ctx.block() == null ? new Block(line, col) : (Block) ctx.block().accept(this);

		ArrayList<Type> superClassGenricList = new ArrayList<Type>();

		ctx.type().forEach(a -> superClassGenricList.add((Type) a.accept(this)));

		ArrayList<ImpliInstance> impls = new ArrayList<ImpliInstance>();
		if (!ctx.implInstance().isEmpty()) {
			ctx.implInstance().forEach(a -> impls.add((ImpliInstance) a.accept(this)));
		}
		

		Block ret = new Block(line, col);
		ret.isolated = true;
		
		ClassDef cd = new ClassDef(line, col, null, false, false, "AnonClassTmp$", new ArrayList<Pair<String, NamedType>>(), null, superclass, superClassGenricList, new ArrayList<Expression>(), impls, classBlock, isAbstract, isFinalDefined, null, false, new ArrayList<Expression>(), false, false, false);
		cd.isAnonClass = true;
		ret.add(cd);
		
		NamedType typeee = new NamedType(line, col, "AnonClassTmp$");
		
		if(isActor) {
			ClassDef actorClass = new ClassDef(line, col, null, false, true, "AnonClassTmp$Actor$", new ArrayList<Pair<String, NamedType>>(), null, null, new ArrayList<Type>(), new ArrayList<Expression>(), new ArrayList<ImpliInstance>(), new Block(line, col), false, null, typeee, true, new ArrayList<Expression>(), false, false, false);
			actorClass.isAnonClass = true;
			ret.add(actorClass);
			typeee = new NamedType(line, col, "AnonClassTmp$Actor$");
		}
		
		New nn = new New(line, col, typeee, new FuncInvokeArgs(line, col), true);
		
		ret.add(new DuffAssign(nn));
		ret.setShouldBePresevedOnStack(true);
		return ret;
		
		/*ClassDef cd = new ClassDef(line, col, null, false, false, null, new ArrayList<Tuple<String, NamedType>>(), null, superclass, superClassGenricList, new ArrayList<Expression>(), impls, classBlock, isAbstract, isFinalDefined, null, false, new ArrayList<Expression>(), false, false, false);
		cd.isLocalClassDef = true;

		AssignExisting ae = new AssignExisting(line, col, "anonClassTmp$", AssignStyleEnum.EQUALS, new LocalClassDef(line, col, cd));
		
		Block ret = new Block(line, col);
		ret.add(ae);
		ret.isolated = true;

		NamedType typeee = new NamedType(line, col, "anonClassTmp$");
		if(isActor) {
			NamedType actType = ScopeAndTypeChecker.const_typed_actor.copyTypeSpecific();
			actType.setLine(line);
			actType.setColumn(col);
			actType.setGenTypes(typeee);
			actType.isDefaultActor = true;
			typeee = actType;
		}
		
		New nn = new New(line, col, typeee, new FuncInvokeArgs(line, col), true);
		
		ret.add(new DuffAssign(nn));
		ret.setShouldBePresevedOnStack(true);
		return ret;*/
		
	}

	@Override
	public DottedNameList visitDottedNameList(ConcurnasParser.DottedNameListContext ctx) {
		DottedNameList ret = new DottedNameList(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		ctx.dotted_name().forEach(a -> ret.add((String) a.accept(this)));

		return ret;
	}

	@Override
	public ArrayList<Expression> visitExpr_stmtList(ConcurnasParser.Expr_stmtListContext ctx) {
		ArrayList<Expression> ret = new ArrayList<Expression>();

		ctx.expr_stmt().forEach(a -> ret.add((Expression) a.accept(this)));

		return ret;
	}

	@Override
	public ClassDefArgs visitClassdefArgs(ConcurnasParser.ClassdefArgsContext ctx) {
		ArrayList<ClassDefArg> args = new ArrayList<ClassDefArg>();

		ctx.classdefArg().forEach(a -> args.add((ClassDefArg) a.accept(this)));

		return new ClassDefArgs(ctx.start.getLine(), ctx.start.getCharPositionInLine(), args);
	}

	@Override
	public ClassDefArg visitClassdefArg(ConcurnasParser.ClassdefArgContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		AccessModifier accessModi = (AccessModifier)(ctx.pppNoInject() != null ? ctx.pppNoInject().accept(this) : AccessModifier.PROTECTED);

		boolean isFinal = ctx.isFinal != null;

		Type type = ctx.type() != null ? (Type) ctx.type().accept(this) : null;

		int refCnt = ctx.refCnt.size();
		if (refCnt > 0) {
			for (int n = 0; n < refCnt; n++) {
				type = new NamedType(line, col, type == null ? NamedType.ntObj : type);
			}
		}

		String prefix = ctx.prefix == null ? null : ctx.prefix.getText();
		ClassDefArg ret = new ClassDefArg(line, col, accessModi, isFinal, prefix, ctx.NAME().getText(), type);

		ret.annotations = ctx.annotations() == null ? null : (Annotations) ctx.annotations().accept(this);
		ret.isVararg = ctx.isvararg != null;
		if (ctx.expr_stmt() != null) {
			ret.defaultValue = (Expression) ctx.expr_stmt().accept(this);
		}
		
		
		if(null != ctx.transAndShared) {
			Thruple<Boolean, Boolean, Boolean> transAndShared = (Thruple<Boolean, Boolean, Boolean>)ctx.transAndShared.accept(this);
			ret.isTransient = transAndShared.getA();
			ret.isShared = transAndShared.getB();
			ret.isLazy = transAndShared.getC();
		}else {
			ret.isTransient = false;
			ret.isShared = false;
			ret.isLazy = false;
		}
		
		ret.isOverride = ctx.override != null;

		return ret;
	}

	@Override
	public Annotations visitAnnotations(ConcurnasParser.AnnotationsContext ctx) {
		ArrayList<Annotation> annotations = new ArrayList<Annotation>();
		ctx.annotation().forEach(a -> annotations.add((Annotation) a.accept(this)));
		return new Annotations(ctx.start.getLine(), ctx.start.getCharPositionInLine(), annotations);
	}

	@Override
	public Annotation visitAnnotation(ConcurnasParser.AnnotationContext ctx) {
		ArrayList<String> locations = new ArrayList<String>();
		ctx.loc.forEach(a -> locations.add(a.getText()));
		String className = (String) ctx.dotted_name().accept(this);

		Expression singleArg = ctx.expr_stmt() == null ? null : (Expression) ctx.expr_stmt().accept(this);

		ArrayList<Pair<String, Expression>> manyArgs = ctx.namedAnnotationArgList() == null ? null : (ArrayList<Pair<String, Expression>>) ctx.namedAnnotationArgList().accept(this);

		return new Annotation(ctx.start.getLine(), ctx.start.getCharPositionInLine(), className, singleArg, manyArgs, locations);
	}

	@Override
	public ArrayList<Pair<String, Expression>> visitNamedAnnotationArgList(ConcurnasParser.NamedAnnotationArgListContext ctx) {
		ArrayList<Pair<String, Expression>> ret = new ArrayList<Pair<String, Expression>>();

		ctx.n2expr().forEach(a -> ret.add((Pair<String, Expression>) a.accept(this)));

		return ret;
	}

	@Override
	public Pair<String, Expression> visitN2expr(ConcurnasParser.N2exprContext ctx) {
		return new Pair<String, Expression>(ctx.NAME().getText(), (Expression) ctx.expr_stmt().accept(this));
	}

	@Override
	public NamedType visitNamedType(ConcurnasParser.NamedTypeContext ctx) {
		if (ctx.isactor != null) {
			if (ctx.namedType_ExActor() == null) {
				return ScopeAndTypeChecker.const_actor.copyTypeSpecific().copyTypeSpecific();
			} else {
				NamedType rhs = (NamedType) ctx.namedType_ExActor().accept(this);
				NamedType actor = ScopeAndTypeChecker.const_typed_actor.copyTypeSpecific();
				actor.setGenTypes(rhs);
				actor.isDefaultActor = true;
				return actor;
			}
		} else {
			return (NamedType) ctx.namedType_ExActor().accept(this);
		}
	}

	@Override
	public NamedType visitNamedType_ExActor(ConcurnasParser.NamedType_ExActorContext ctx) {
		String primaryName = (String) ctx.primaryName.accept(this);
		ArrayList<Type> genTypes = ctx.priamryGens != null ? (ArrayList<Type>) ctx.priamryGens.accept(this) : new ArrayList<Type>();

		ArrayList<Pair<String, ArrayList<Type>>> nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();

		for (ConcurnasParser.NameAndgensContext item : ctx.nameAndgens()) {
			nestorSegments.add(new Pair<String, ArrayList<Type>>(primaryName, genTypes));

			Pair<String, ArrayList<Type>> reso = (Pair<String, ArrayList<Type>>) item.accept(this);
			primaryName = reso.getA();
			genTypes = reso.getB();
		}

		NamedType ret;
		if (genTypes.isEmpty()) {
			ret = new NamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), primaryName);
		} else {
			ret = new NamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), primaryName, genTypes, nestorSegments);
		}

		if (ctx.of != null) {
			NamedType actorof = (NamedType) ctx.of.accept(this);
			ret.getGenTypes().add(0, actorof);
			ret.expectedToBeAbstractTypedActor = true;
		}

		return ret;
	}

	@Override
	public Pair<String, ArrayList<Type>> visitNameAndgens(ConcurnasParser.NameAndgensContext ctx) {
		ArrayList<Type> gens = ctx.genTypeList() == null ? new ArrayList<Type>() : (ArrayList<Type>) ctx.genTypeList().accept(this);
		return new Pair<String, ArrayList<Type>>(ctx.NAME().getText(), gens);
	}

	@Override
	public ArrayList<Type> visitGenTypeList(ConcurnasParser.GenTypeListContext ctx) {
		ArrayList<Type> ret = new ArrayList<Type>();
		ctx.genTypeListElemnt().forEach(a -> ret.add((Type) a.accept(this)));
		return ret;
	}

	@Override
	public Type visitGenTypeListElemnt(ConcurnasParser.GenTypeListElemntContext ctx) {
		Type got;
		if (ctx.type() == null) {// wildcard
			got = new GenericType("?", -1);
		} else {
			got = (Type) ctx.type().accept(this);

			if (ctx.inoutGenericModifier() != null) {
				got.setInOutGenModifier((InoutGenericModifier) ctx.inoutGenericModifier().accept(this));
			}
		}
		return got;
	}

	@Override
	public InoutGenericModifier visitInoutGenericModifier(ConcurnasParser.InoutGenericModifierContext ctx) {
		return ctx.getText().equals("in") ? InoutGenericModifier.IN : InoutGenericModifier.OUT;
	}

	@Override
	public VarNull visitNullNode(ConcurnasParser.NullNodeContext ctx) {
		return new VarNull(ctx.start.getLine(), ctx.start.getCharPositionInLine());
	}

	@Override
	public AnnotationDef visitAnnotationDef(ConcurnasParser.AnnotationDefContext ctx) {
		AccessModifier accessModi = (AccessModifier) (ctx.pppNoInject() != null ? ctx.pppNoInject().accept(this) : null);

		Block annotBlock = ctx.block() == null ? new Block(ctx.start.getLine(), ctx.start.getCharPositionInLine()) : (Block) ctx.block().accept(this);

		ArrayList<AnnotationDefArg> annotationDefArgs = new ArrayList<AnnotationDefArg>();
		ctx.annotationArg().forEach(a -> annotationDefArgs.add((AnnotationDefArg) a.accept(this)));

		return new AnnotationDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), accessModi, ctx.NAME().getText(), annotBlock, annotationDefArgs);
	}

	@Override
	public AnnotationDefArg visitAnnotationArg(ConcurnasParser.AnnotationArgContext ctx) {
		Annotations annot = ctx.annotations() == null ? null : (Annotations) ctx.annotations().accept(this);

		Type optionalType = ctx.type() == null ? null : (Type) ctx.type().accept(this);

		Expression expression = ctx.expr_stmt() == null ? null : (Expression) ctx.expr_stmt().accept(this);

		return new AnnotationDefArg(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME().getText(), optionalType, expression, annot);

	}

	@Override
	public EnumDef visitEnumdef(ConcurnasParser.EnumdefContext ctx) {
		AccessModifier accessModi = (AccessModifier)(ctx.pppNoInject() != null ? ctx.pppNoInject().accept(this) : null);
		EnumBlock enumblock = (EnumBlock) ctx.enumblock().accept(this);

		ClassDefArgs classDefArgs = ctx.classdefArgs() == null ? null : (ClassDefArgs) ctx.classdefArgs().accept(this);

		return new EnumDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), accessModi, ctx.NAME().getText(), classDefArgs, enumblock);
	}

	@Override
	public EnumBlock visitEnumblock(ConcurnasParser.EnumblockContext ctx) {
		ArrayList<EnumItem> enumItemz = new ArrayList<EnumItem>();
		ctx.enumItem().forEach(a -> enumItemz.add((EnumItem) a.accept(this)));

		Block mainBlock = new Block(ctx.start.getLine(), ctx.start.getCharPositionInLine());
		ctx.line().forEach(a -> mainBlock.addAll((ArrayList<LineHolder>) a.accept(this)));

		return new EnumBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), enumItemz, mainBlock);
	}

	@Override
	public EnumItem visitEnumItem(ConcurnasParser.EnumItemContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Block block = ctx.block() == null ? null : (Block) ctx.block().accept(this);

		FuncInvokeArgs args = ctx.pureFuncInvokeArgs() == null ? new FuncInvokeArgs(line, col) : (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);

		EnumItem ret = new EnumItem(line, col, ctx.NAME().getText(), args, block);
		if (null != ctx.annotations()) {
			ret.annotations = (Annotations) ctx.annotations().accept(this);
		}
		return ret;
	}

	// FuncRefArgs

	@Override
	public FuncRefArgs visitFuncRefArgs(ConcurnasParser.FuncRefArgsContext ctx) {
		FuncRefArgs ret = new FuncRefArgs(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		for (ConcurnasParser.FuncRefArgContext pfic : ctx.funcRefArg()) {
			Pair<String, Object> res = (Pair<String, Object>) pfic.accept(this);
			String name = res.getA();
			if (null != name) {
				ret.addName(name, res.getB());
			} else {
				Object resto = res.getB();
				if (resto instanceof Type) {
					ret.addType((Type) resto);
				} else {
					ret.addExpr((Expression) resto);
				}
			}
		}

		return ret;
	}

	@Override
	public Pair<String, Object> visitFuncRefArg(ConcurnasParser.FuncRefArgContext ctx) {
		Object expr;
		if (ctx.expr_stmt() != null) {
			expr = ctx.expr_stmt().accept(this);
		} else if (ctx.primitiveType() != null) {
			expr = ctx.primitiveType().accept(this);
		} else if (ctx.type() != null) {
			expr = ctx.type().accept(this);
		} else if (ctx.funcType() != null) {
			expr = ctx.funcType().accept(this);
		} else if(ctx.namedType() != null) {
			expr = ctx.namedType().accept(this);
		} else if(ctx.tupleType() != null) {
			expr = ctx.tupleType().accept(this);
		} else {
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			int refcnts = ctx.refcnt.size();
			expr = null;
			for (int n = 0; n < refcnts; n++) {
				expr = new NamedType(line, col, expr == null ? NamedType.ntObj : (Type) expr);
			}
		}
		if(ctx.lazy != null) {
			expr = Utils.convertToLazyType((Type)expr);
		}
		
		if(ctx.nullable != null) {
			((Type)expr).setNullStatus(NullStatus.NULLABLE);
		}

		return new Pair<String, Object>(ctx.NAME() == null ? null : ctx.NAME().getText(), expr);
	}

	@Override
	public FuncInvokeArgs visitPureFuncInvokeArgs(ConcurnasParser.PureFuncInvokeArgsContext ctx) {
		FuncInvokeArgs ret = new FuncInvokeArgs(ctx.start.getLine(), ctx.start.getCharPositionInLine());

		for (ConcurnasParser.PureFuncInvokeArgContext pfic : ctx.pureFuncInvokeArg()) {
			Pair<String, Expression> res = (Pair<String, Expression>) pfic.accept(this);
			String name = res.getA();
			if (null != name) {
				ret.addName(name, res.getB());
			} else {
				ret.add(res.getB());
			}
		}

		return ret;
	}

	@Override
	public Pair<String, Expression> visitPureFuncInvokeArg(ConcurnasParser.PureFuncInvokeArgContext ctx) {
		Expression expr;
		if (ctx.expr_stmt() != null) {
			expr = (Expression) ctx.expr_stmt().accept(this);
		} else if (ctx.primitiveType() != null) {
			expr = new TypeReturningExpression((Type) ctx.primitiveType().accept(this));
		} else {
			expr = new TypeReturningExpression((Type) ctx.funcType().accept(this));
		}

		return new Pair<String, Expression>(ctx.NAME() == null ? null : ctx.NAME().getText(), expr);
	}

	@Override
	public ConstructorDef visitConstructorDef(ConcurnasParser.ConstructorDefContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Pair<AccessModifier, Boolean> accessModifierAndInject = (Pair<AccessModifier, Boolean>)(ctx.ppp() != null ? ctx.ppp().accept(this) : null);
		AccessModifier accessModi = accessModifierAndInject == null?null:accessModifierAndInject.getA();
		
		FuncParams params = ctx.funcParams() == null ? new FuncParams(line, col) : (FuncParams) ctx.funcParams().accept(this);

		Block bbk;
		if (ctx.block() != null) {
			bbk = (Block) ctx.block().accept(this);
		} else {
			bbk = (Block) ctx.single_line_block().accept(this);
		}

		ConstructorDef cd = new ConstructorDef(line, col, accessModi, params, bbk);
		cd.isInjected = accessModifierAndInject==null?false:accessModifierAndInject.getB();
		
		return cd;
	}

	@Override
	public ForBlockVariant visitForblockvariant(ConcurnasParser.ForblockvariantContext ctx) {

		String fbv = ctx.getText();
		switch (fbv) {
		case "parfor":
			return ForBlockVariant.PARFOR;
		case "parforsync":
			return ForBlockVariant.PARFORSYNC;
		default:
			return null;
		}
	}

	@Override
	public CompoundStatement visitFor_stmt_old(ConcurnasParser.For_stmt_oldContext ctx) {
		ForBlockVariant pfv = (ForBlockVariant) ctx.forblockvariant().accept(this);

		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		ForBlockOld ret = new ForBlockOld(line, col, ctx.assignExpr == null ? null : (Expression) ctx.assignExpr.accept(this), ctx.NAME() == null ? null : ctx.NAME().getText(), ctx.type() == null ? null : (Type) ctx.type().accept(this), ctx.assignStyle() == null ? null : (AssignStyleEnum) ctx.assignStyle().accept(this), ctx.assigFrom == null ? null : (Expression) ctx.assigFrom.accept(this), ctx.check == null ? null : (Expression) ctx.check.accept(this), ctx.postExpr == null ? null : (Expression) ctx.postExpr.accept(this), (Block) ctx.mainblock.accept(this), pfv);

		if (null != ctx.elseblock) {
			ret.elseblock = (Block) ctx.elseblock.accept(this);
		}
		
		if(pfv != null) {
			return convertForIfParfor(line, col, pfv, ret);
		}

		return ret;
	}

	@Override
	public Statement visitCompound_stmt_atomic_base(ConcurnasParser.Compound_stmt_atomic_baseContext ctx) {
		Statement ret = (Statement) ctx.getChild(0).accept(this);
		ret.setShouldBePresevedOnStack(true);
		return ret;
	}

	@Override
	public Pair<String, Type> visitForVarTupleOrNothing(ConcurnasParser.ForVarTupleOrNothingContext ctx) {
		ConcurnasParser.ForVarTupleContext tt = ctx.forVarTuple() ;
		if(tt == null) {
			return null;
		}else {
			return (Pair<String, Type>)tt.accept(this);
		}
	}

	@Override
	public Pair<String, Type> visitForVarTuple(ConcurnasParser.ForVarTupleContext ctx) {
		String name = ctx.localVarName.getText();
		Type tt = ctx.localVarType == null?null:(Type)ctx.localVarType.accept(this);
		return new Pair<String, Type>(name, tt);
	}

	private AssignTupleDeref makeAssignTupleDerefForLoop(int line, int col, List<ConcurnasParser.ForVarTupleOrNothingContext> inputs) {
		ArrayList<Assign> lhss = new ArrayList<Assign>();
		for(ConcurnasParser.ForVarTupleOrNothingContext tupComp: inputs) {
			Pair<String, Type> inst = (Pair<String, Type>)tupComp.accept(this);
			Assign ass = null;
			if(inst != null) {
				String name = inst.getA();
				Type type = inst.getB();
				
				if(type == null) {
					ass = new AssignExisting(line, col, name, null, null);
				}else {
					ass = new AssignNew(null, line, col, name, type);
				}
			}
			lhss.add(ass);
		}
		return new AssignTupleDeref(line, col, lhss, AssignStyleEnum.EQUALS, null);
	}
	
	@Override
	public CompoundStatement visitFor_stmt(ConcurnasParser.For_stmtContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		ForBlockVariant pfv = (ForBlockVariant) ctx.forblockvariant().accept(this);
		
		ForBlock ret;
		if(!ctx.forVarTupleOrNothing().isEmpty()) {
			AssignTupleDeref assignTup =  makeAssignTupleDerefForLoop(line, col, ctx.forVarTupleOrNothing());
			ret = new ForBlock(line, col, assignTup, (Expression) ctx.expr.accept(this), (Block) ctx.mainblock.accept(this), pfv);
		}else {
			ret = new ForBlock(line, col, ctx.localVarName.getText(), ctx.localVarType == null ? null : (Type) ctx.localVarType.accept(this), (Expression) ctx.expr.accept(this), (Block) ctx.mainblock.accept(this), pfv);
		}
		
		if (null != ctx.elseblock) {
			ret.elseblock = (Block) ctx.elseblock.accept(this);
		}

		if (ctx.idxName != null) {
			if(ctx.idxType == null && ctx.idxExpr == null) {
				ret.idxVariableAssignment = new RefName(line, col, ctx.idxName.getText());
			}else {
				Expression expr = ctx.idxExpr == null ?  new VarInt(line, col, 0) : (Expression) ctx.idxExpr.accept(this);
				ret.idxVariableCreator = new AssignNew(null, line, col, false, false, ctx.idxName.getText(), ctx.idxType == null ? null : (Type) ctx.idxType.accept(this), AssignStyleEnum.EQUALS, expr);
			}
		}
		
		if(pfv != null) {
			return convertForIfParfor(line, col, pfv, ret);
		}
		
		return ret;
	}

	private CompoundStatement convertForIfParfor(int line, int col, ForBlockVariant pfv, ForBlockMaybeParFor ret) {
		Block contents = ret.getMainBlock();
		int linex = contents.getLine();
		int colx = contents.getColumn();
		
		Block newContents = new Block(linex, colx);
		
		Block asyncBlockBody = new Block(linex, colx);
		asyncBlockBody.isolated = true;
		asyncBlockBody.isAsyncBody=true;
		asyncBlockBody.addAll(contents.lines);
		
		newContents.add(new AsyncBlock(line, col, asyncBlockBody));
		
		ret.setMainBlock(newContents);
		
		CompoundStatement toretrun = (CompoundStatement)ret;
		
		if(pfv == ForBlockVariant.PARFORSYNC) {
			Block syncBlock = new Block(linex, colx);
			syncBlock.add((Line)ret);
			toretrun = createSyncBlock( line,  col,  syncBlock);
		}
		
		toretrun.setShouldBePresevedOnStack(((Node)ret).getShouldBePresevedOnStack());
		
		return toretrun;
	}
	
	@Override
	public MatchStatement visitMatch_stmt(ConcurnasParser.Match_stmtContext ctx) {
		ArrayList<MactchCase> cases = new ArrayList<MactchCase>();

		ctx.match_case_stmt().forEach(a -> cases.add((MactchCase) a.accept(this)));

		Block elseblock = null;
		if (ctx.elseb != null) {
			elseblock = (Block) ctx.elseb.accept(this);
		} else if (ctx.elsebs != null) {
			elseblock = (Block) ctx.elsebs.accept(this);
		}

		return new MatchStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Statement) ctx.simple_stmt().accept(this) /* RefName matchon */, cases, elseblock);
	}

	@Override
	public MactchCase visitMatch_case_stmt(ConcurnasParser.Match_case_stmtContext ctx) {
		return (MactchCase) (ctx.match_case_stmt_case() != null ? ctx.match_case_stmt_case().accept(this) : ctx.match_case_stmt_nocase().accept(this));
	}

	@Override
	public MactchCase visitMatch_case_stmt_case(ConcurnasParser.Match_case_stmt_caseContext ctx) {
		Block blk = (Block) (ctx.block() != null ? ctx.block().accept(this) : ctx.single_line_block().accept(this));

		CaseExpression ce;
		if (ctx.match_case_stmt_assign() != null) {
			ce = (CaseExpression) ctx.match_case_stmt_assign().accept(this);
		} else if(ctx.match_case_assign_typedObjectAssign() != null) { 
			ce = (CaseExpression) ctx.match_case_assign_typedObjectAssign().accept(this);
		}else if (ctx.match_case_stmt_typedCase() != null) {
			ce = (CaseExpression) ctx.match_case_stmt_typedCase().accept(this);
		} else if (null != ctx.case_expr_chain_or()) {
			ce = (CaseExpression) ctx.case_expr_chain_or().accept(this);
		} else if(null !=  ctx.match_case_stmt_assignTuple()){	
			ce = (CaseExpression) ctx.match_case_stmt_assignTuple().accept(this);		
		}else if(null != ctx.case_expr_chain_Tuple()) {
			ce = (CaseExpression) ctx.case_expr_chain_Tuple().accept(this);
		}else {
			Expression alsoCond = (Expression) ctx.justAlso.accept(this);
			ce = new JustAlsoCaseExpression(alsoCond.getLine(), alsoCond.getColumn(), alsoCond);
		}

		if (ctx.matchAlso != null) {
			ce.alsoCondition = (Expression) ctx.matchAlso.accept(this);
		}

		return new MactchCase(ce, blk);
	}

	@Override
	public MactchCase visitMatch_case_stmt_nocase(ConcurnasParser.Match_case_stmt_nocaseContext ctx) {
		Block blk = (Block) (ctx.block() != null ? ctx.block().accept(this) : ctx.single_line_block().accept(this));

		CaseExpression ce;
		if (ctx.match_case_stmt_assign() != null) {
			ce = (CaseExpression) ctx.match_case_stmt_assign().accept(this);
		} else if(ctx.match_case_assign_typedObjectAssign() != null) { 
			ce = (CaseExpression) ctx.match_case_assign_typedObjectAssign().accept(this);
		}else if (ctx.match_case_stmt_typedCase() != null) {
			ce = (CaseExpression) ctx.match_case_stmt_typedCase().accept(this);
		} else if (null != ctx.case_expr_chain_or()) {
			ce = (CaseExpression) ctx.case_expr_chain_or().accept(this);
		}else if(null !=  ctx.match_case_stmt_assignTuple()){	
			ce = (CaseExpression) ctx.match_case_stmt_assignTuple().accept(this);
		}else if(null != ctx.case_expr_chain_Tuple()) {
			ce = (CaseExpression) ctx.case_expr_chain_Tuple().accept(this);
		}else {
			Expression alsoCond = (Expression) ctx.justAlso.accept(this);
			ce = new JustAlsoCaseExpression(alsoCond.getLine(), alsoCond.getColumn(), alsoCond);
		}

		if (ctx.matchAlso != null) {
			ce.alsoCondition = (Expression) ctx.matchAlso.accept(this);
		}

		return new MactchCase(ce, blk);
	}
	
	@Override
	public CaseExpressionObjectTypeAssign visitMatch_case_assign_typedObjectAssign(ConcurnasParser.Match_case_assign_typedObjectAssignContext ctx) {
		
		String varname = ctx.NAME().getText();
		Expression expr = (Expression)ctx.bitwise_or().accept(this);

		boolean forceNew = ctx.var != null || ctx.isfinal != null;

		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		
		return new CaseExpressionObjectTypeAssign(line, col, varname, new CaseExpressionWrapper(line, col, expr), forceNew, ctx.isfinal != null);
	}

	@Override
	public CaseExpressionAssign visitMatch_case_stmt_assign(ConcurnasParser.Match_case_stmt_assignContext ctx) {
		ArrayList<Type> types = new ArrayList<Type>();

		ctx.type().forEach(a -> types.add((Type) a.accept(this)));

		boolean forceNew = ctx.var != null || ctx.isfinal != null;

		return new CaseExpressionAssign(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME().getText(), ctx.expr_stmt() == null ? null : (Expression) ctx.expr_stmt().accept(this), types, forceNew, ctx.isfinal != null);
	}

	
	@Override
	public Pair<String, Type> visitMatchTupleAsignOrNone(ConcurnasParser.MatchTupleAsignOrNoneContext ctx) {
		ConcurnasParser.MatchTupleAsignContext inst = ctx.matchTupleAsign();
		return inst == null?null:(Pair<String, Type>)inst.accept(this);
	}

	@Override
	public Pair<String, Type> visitMatchTupleAsign(ConcurnasParser.MatchTupleAsignContext ctx) {
		String name =ctx.NAME().getText();
		Type type = /*ctx.type()==null?null:*/(Type)ctx.type().accept(this);
		
		return new Pair<String, Type>(name, type);
	}
	
	
	@Override
	public CaseExpressionAssignTuple visitMatch_case_stmt_assignTuple(ConcurnasParser.Match_case_stmt_assignTupleContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		
		ArrayList<Assign> lhss = new ArrayList<Assign>();
		for(ConcurnasParser.MatchTupleAsignOrNoneContext tupComp: ctx.matchTupleAsignOrNone()) {
			Pair<String, Type> inst = (Pair<String, Type>)tupComp.accept(this);
			Assign ass = null;
			if(inst != null) {
				String name = inst.getA();
				Type type = inst.getB();
				
				/*if(type == null) {
					ass = new AssignExisting(line, col, name, null, null);
				}else {*/
					ass = new AssignNew(null, line, col, name, type);
				//}
			}
			lhss.add(ass);
		}
		
		return new CaseExpressionAssignTuple(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.expr_stmt() == null ? null : (Expression) ctx.expr_stmt().accept(this), lhss);
	}
	
	
	@Override
	public CaseExpression visitCase_expr_chain_Tuple(ConcurnasParser.Case_expr_chain_TupleContext ctx) {
		List<ConcurnasParser.Case_expr_chain_orOrNoneContext> itemsz = ctx.case_expr_chain_orOrNone();
		ArrayList<CaseExpression> caseOrs = new ArrayList<CaseExpression>(itemsz.size());
		
		for(ConcurnasParser.Case_expr_chain_orOrNoneContext inst : itemsz) {
			caseOrs.add((CaseExpression)inst.accept(this));
		}
		
		return new CaseExpressionTuple(ctx.start.getLine(), ctx.start.getCharPositionInLine(), caseOrs);
	}
	
	
	@Override
	public CaseExpression visitCase_expr_chain_orOrNone(ConcurnasParser.Case_expr_chain_orOrNoneContext ctx) {
		ConcurnasParser.Case_expr_chain_orContext item = ctx.case_expr_chain_or();
		return item == null?null:(CaseExpression)item.accept(this);
	}
	
	@Override
	public TypedCaseExpression visitMatch_case_stmt_typedCase(ConcurnasParser.Match_case_stmt_typedCaseContext ctx) {
		ArrayList<Type> types = new ArrayList<Type>();

		ctx.type().forEach(a -> types.add((Type) a.accept(this)));

		CaseExpression caseExpression = null;
		
		if(ctx.case_expr_chain_Tuple() != null) {
			caseExpression = (CaseExpression) ctx.case_expr_chain_Tuple().accept(this);
		}else if(ctx.case_expr_chain_or() != null) {
			caseExpression = (CaseExpression) ctx.case_expr_chain_or().accept(this);
		}
		
		//CaseExpression caseExpression = ctx.case_expr_chain_Tuple() == null ? null : (CaseExpression) ctx.case_expr_chain_Tuple().accept(this);

		return new TypedCaseExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), types, caseExpression);
	}

	@Override
	public CaseExpression visitCase_expr_chain_or(ConcurnasParser.Case_expr_chain_orContext ctx) {
		ArrayList<CaseExpression> caseOrs = new ArrayList<CaseExpression>();

		ctx.case_expr_chain_and().forEach(a -> caseOrs.add((CaseExpression) a.accept(this)));

		if (caseOrs.size() == 1) {
			return caseOrs.remove(0);
		} else {
			return new CaseExpressionOr(ctx.start.getLine(), ctx.start.getCharPositionInLine(), caseOrs.remove(0), caseOrs);
		}
	}

	@Override
	public CaseExpression visitCase_expr_chain_and(ConcurnasParser.Case_expr_chain_andContext ctx) {
		ArrayList<CaseExpression> caseOrs = new ArrayList<CaseExpression>();

		ctx.case_expr().forEach(a -> caseOrs.add((CaseExpression) a.accept(this)));

		if (caseOrs.size() == 1) {
			return caseOrs.remove(0);
		} else {
			return new CaseExpressionAnd(ctx.start.getLine(), ctx.start.getCharPositionInLine(), caseOrs.remove(0), caseOrs);
		}
	}

	@Override
	public CaseExpression visitCase_expr(ConcurnasParser.Case_exprContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Expression expr = (Expression) ctx.bitwise_or().accept(this);
		CaseExpression ret;
		if (ctx.case_operator_pre() != null) {
			ret = new CaseExpressionPre(line, col, (CaseOperatorEnum) ctx.case_operator_pre().accept(this), expr);
		} else {
			if (ctx.case_operator() != null) {
				ret = new CaseExpressionPost(line, col, (CaseOperatorEnum) ctx.case_operator().accept(this), expr);
			} else {
				ret = new CaseExpressionWrapper(line, col, expr);
			}
		}

		return ret;
	}

	@Override
	public CaseOperatorEnum visitCase_operator(ConcurnasParser.Case_operatorContext ctx) {
		return CaseOperatorEnum.symToCaseOperatorEnum.get(ctx.getText());
	}

	@Override
	public CaseOperatorEnum visitCase_operator_pre(ConcurnasParser.Case_operator_preContext ctx) {
		String txt = ctx.getText();
		return CaseOperatorEnum.symToCaseOperatorEnum.get(txt);
	}

	@Override
	public IfStatement visitIf_stmt(ConcurnasParser.If_stmtContext ctx) {
		ArrayList<ElifUnit> elifunits = new ArrayList<ElifUnit>();

		ctx.elifUnit().forEach(a -> elifunits.add((ElifUnit) a.accept(this)));

		return new IfStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.ifexpr.accept(this), (Block) ctx.ifblk.accept(this), elifunits, ctx.elseblk == null ? null : (Block) ctx.elseblk.accept(this));
	}

	@Override
	public ElifUnit visitElifUnit(ConcurnasParser.ElifUnitContext ctx) {
		return new ElifUnit(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.expr_stmt().accept(this), (Block) ctx.block().accept(this));
	}

	@Override
	public AsyncBodyBlock visitAsync_block(ConcurnasParser.Async_blockContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		ArrayList<Block> preBlocks = new ArrayList<Block>();
		ArrayList<Block> postBlocks = new ArrayList<Block>();
		Block mainBody = new Block(line, col);

		ctx.preblk.forEach(a -> preBlocks.add((Block) a.accept(this)));
		ctx.postblk.forEach(a -> postBlocks.add((Block) a.accept(this)));
		ctx.line().forEach(a -> mainBody.addAll((ArrayList<LineHolder>) a.accept(this)));

		return new AsyncBodyBlock(line, col, preBlocks, postBlocks, mainBody);
	}

	@Override
	public WhileBlock visitWhile_stmt(ConcurnasParser.While_stmtContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		WhileBlock ret = new WhileBlock(line, col, (Expression) ctx.mainExpr.accept(this), (Block) ctx.mainBlock.accept(this));

		if (null != ctx.elseblock) {
			ret.elseblock = (Block) ctx.elseblock.accept(this);
		}

		if (ctx.idxName != null) {
			if(ctx.idxType == null && ctx.idxExpr == null) {
				ret.idxVariableAssignment = new RefName(line, col, ctx.idxName.getText());
			}else {
				Expression expr = ctx.idxExpr == null ?  new VarInt(line, col, 0) : (Expression) ctx.idxExpr.accept(this);
				ret.idxVariableCreator = new AssignNew(null, line, col, false, false, ctx.idxName.getText(), ctx.idxType == null ? null : (Type) ctx.idxType.accept(this), AssignStyleEnum.EQUALS, expr);
			}
		}

		return ret;
	}

	@Override
	public WhileBlock visitLoop_stmt(ConcurnasParser.Loop_stmtContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		WhileBlock ret = new WhileBlock(line, col, new RefBoolean(line, col, true), (Block) ctx.mainBlock.accept(this));

		if (ctx.idxName != null) {
			if(ctx.idxType == null && ctx.idxExpr == null) {
				ret.idxVariableAssignment = new RefName(line, col, ctx.idxName.getText());
			}else {
				Expression expr = ctx.idxExpr == null ?  new VarInt(line, col, 0) : (Expression) ctx.idxExpr.accept(this);
				ret.idxVariableCreator = new AssignNew(null, line, col, false, false, ctx.idxName.getText(), ctx.idxType == null ? null : (Type) ctx.idxType.accept(this), AssignStyleEnum.EQUALS, expr);
			}
		}

		return ret;
	}

	@Override
	public TryCatch visitTry_stmt(ConcurnasParser.Try_stmtContext ctx) {
		ArrayList<CatchBlocks> cbs = new ArrayList<CatchBlocks>();
		ctx.catchBlock().forEach(a -> cbs.add((CatchBlocks) a.accept(this)));

		ArrayList<Line> tryWithResources = new ArrayList<Line>();
		ctx.simple_stmt().forEach(a -> tryWithResources.add((Line) a.accept(this)));

		TryCatch ret = new TryCatch(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Block) ctx.mainblock.accept(this), cbs, ctx.finblock == null ? null : (Block) ctx.finblock.accept(this));
		ret.tryWithResources = tryWithResources;
		return ret;
	}

	@Override
	public CatchBlocks visitCatchBlock(ConcurnasParser.CatchBlockContext ctx) {
		ArrayList<Type> caughtTypes = new ArrayList<Type>();
		ctx.type().forEach(a -> caughtTypes.add((Type) a.accept(this)));

		return new CatchBlocks(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME().getText(), caughtTypes, (Block) ctx.block().accept(this));
	}

	@Override
	public Block visitBlock_async(ConcurnasParser.Block_asyncContext ctx) {

		Block mainBlock = (Block) ctx.block_().accept(this);
		mainBlock.isolated = true;
		if (ctx.async != null) {
			if (ctx.executor != null) {
				mainBlock = new AsyncBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), mainBlock, true, (Expression) ctx.executor.accept(this));
			} else {
				mainBlock = new AsyncBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), mainBlock, true);
			}

		} else {
			mainBlock.canContainAReturnStmt = null;
		}

		return mainBlock;
	}

	@Override
	public WithBlock visitWith_stmt(ConcurnasParser.With_stmtContext ctx) {
		return new WithBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression)ctx.expr_stmt().accept(this), (Block) ctx.block().accept(this));
	}

	@Override
	public TransBlock visitTrans_block(ConcurnasParser.Trans_blockContext ctx) {
		return new TransBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Block) ctx.block().accept(this));
	}

	@Override
	public InitBlock visitInit_block(ConcurnasParser.Init_blockContext ctx) {
		return new InitBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Block) ctx.block().accept(this));
	}

	private TryCatch createSyncBlock(int line, int col, Block b) {
		ArrayList<Expression> beginCall = new ArrayList<Expression>();
		beginCall.add(new RefName(line, col, "com"));
		beginCall.add(new RefName(line, col, "concurnas"));
		beginCall.add(new RefName(line, col, "bootstrap"));
		beginCall.add(new RefName(line, col, "runtime"));
		beginCall.add(new RefName(line, col, "cps"));
		beginCall.add(new RefName(line, col, "Fiber"));
		beginCall.add(new FuncInvoke(line, col, "getCurrentFiber", new FuncInvokeArgs(line, col)));
		beginCall.add(new FuncInvoke(line, col, "enterSync", new FuncInvokeArgs(line, col)));
		b.reallyPrepend(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, beginCall))));

		ArrayList<Expression> exitSyncCall = new ArrayList<Expression>();
		exitSyncCall.add(new RefName(line, col, "com"));
		exitSyncCall.add(new RefName(line, col, "concurnas"));
		exitSyncCall.add(new RefName(line, col, "bootstrap"));
		exitSyncCall.add(new RefName(line, col, "runtime"));
		exitSyncCall.add(new RefName(line, col, "cps"));
		exitSyncCall.add(new RefName(line, col, "Fiber"));
		exitSyncCall.add(new FuncInvoke(line, col, "getCurrentFiber", new FuncInvokeArgs(line, col)));
		exitSyncCall.add(new FuncInvoke(line, col, "exitsync", new FuncInvokeArgs(line, col)));

		Block finBlock = new Block(line, col);
		finBlock.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, exitSyncCall))));

		TryCatch ret = new TryCatch(line, col, b, new ArrayList<CatchBlocks>(), finBlock);
		ret.isReallyA = "sync";
		return ret;
	}
	
	@Override
	public TryCatch visitSync_block(ConcurnasParser.Sync_blockContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Block b = (Block) ctx.block().accept(this);

		return createSyncBlock( line,  col,  b);
	}

	@Override
	public OnChange visitOnchange(ConcurnasParser.OnchangeContext ctx) {
		List<String> extraArgs = ctx.opts.stream().map(a -> a.getText()).collect(Collectors.toList());
		
		/*Block body;
		if(null != ctx.block()) {
			body = (Block) ctx.block().accept(this);
		}else {
			body = (Block)ctx.single_line_block().accept(this);
		}*/
		Block body = (Block) ctx.block().accept(this);
		
		return new OnChange(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.onChangeEtcArgs() == null ? new ArrayList<Node>() : (ArrayList<Node>) ctx.onChangeEtcArgs().accept(this), body, extraArgs);
	}

	@Override
	public OnEvery visitEvery(ConcurnasParser.EveryContext ctx) {
		List<String> extraArgs = ctx.opts.stream().map(a -> a.getText()).collect(Collectors.toList());
		
		/*Block body;
		if(null != ctx.block()) {
			body = (Block) ctx.block().accept(this);
		}else {
			body = (Block)ctx.single_line_block().accept(this);
		}*/
		Block body = (Block) ctx.block().accept(this);
		
		return new OnEvery(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.onChangeEtcArgs() == null ? new ArrayList<Node>() : (ArrayList<Node>) ctx.onChangeEtcArgs().accept(this), body, extraArgs);
	}

	@Override
	public ArrayRefElement visitArrayRefElement(ConcurnasParser.ArrayRefElementContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		ArrayRefElement ret;
		if (ctx.simple != null) {
			ret = new ArrayRefElement(line, col, (Expression) ctx.simple.accept(this));
		} else if (ctx.pre != null) {
			ret = new ArrayRefElementPrefixAll(line, col, (Expression) ctx.pre.accept(this));
		} else if (ctx.post != null) {
			ret = new ArrayRefElementPostfixAll(line, col, (Expression) ctx.post.accept(this));
		} else {
			ret = new ArrayRefElementSubList(line, col, (Expression) ctx.lhs.accept(this), (Expression) ctx.rhs.accept(this));
		}

		return ret;
	}

	@Override
	public RefOf visitOfNode(ConcurnasParser.OfNodeContext ctx) {
		return new RefOf(ctx.start.getLine(), ctx.start.getCharPositionInLine());
	}

	@Override
	public RefThis visitThisNode(ConcurnasParser.ThisNodeContext ctx) {
		String thisQuali = null;

		if (ctx.thisQuali != null) {
			thisQuali = (String) ctx.thisQuali.accept(this);
		} else if (ctx.thisQualiPrim != null) {
			thisQuali = ctx.thisQualiPrim.getText();
		}

		return new RefThis(ctx.start.getLine(), ctx.start.getCharPositionInLine(), thisQuali);
	}

	@Override
	public Changed visitChangedNode(ConcurnasParser.ChangedNodeContext ctx) {
		return new Changed(ctx.start.getLine(), ctx.start.getCharPositionInLine());
	}

	@Override
	public DotOperator visitSuperNode(ConcurnasParser.SuperNodeContext ctx) {// JPT: copy past of above
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		ArrayList<Expression> elements = new ArrayList<Expression>();
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>();
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>();
		ArrayList<Boolean> isSafeCall = new ArrayList<Boolean>();

		String superQuali = null;
		if(ctx.superQuali != null) {
			superQuali = ctx.superQuali.getText();
		}
		
		Expression head = new RefSuper(line, col, superQuali);

		int n = 0;
		for (ConcurnasParser.Expr_stmt_BelowDotContext itm : ctx.expr_stmt_BelowDot()) {
			Expression expr = (Expression) itm.accept(this);
			if (head == null) {
				head = expr;
			} else {
				elements.add(expr);
				String dddop = ctx.dotOpArg(n++).getText();
				isDirectAccess.add(dddop.equals("\\."));
				returnCalledOn.add(dddop.equals(".."));
				isSafeCall.add(dddop.equals("?."));
			}
		}
		
		return new DotOperator(line, col, head, elements, isDirectAccess, returnCalledOn, isSafeCall);
	}

	@Override
	public Expression visitArrayDef(ConcurnasParser.ArrayDefContext ctx) {

		if (ctx.arrayDefComplex() != null) {
			return (Expression) ctx.arrayDefComplex().accept(this);
		}

		ArrayList<Expression> arrayElements = new ArrayList<Expression>();
		ctx.expr_stmt().forEach(a -> arrayElements.add((Expression) a.accept(this)));
		ArrayDef ret = new ArrayDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), arrayElements);

		if (ctx.isArray != null) {
			ret.isArray = true;
			ret.supressArrayConcat = true;
		}

		return ret;
	}

	@Override
	public Expression visitArrayDefComplex(ConcurnasParser.ArrayDefComplexContext ctx) {
		ArrayList<ArrayList<Expression>> subarrays = new ArrayList<ArrayList<Expression>>();

		ArrayList<Expression> first = new ArrayList<Expression>();
		ctx.expr_stmt_().forEach(a -> first.add((Expression) a.accept(this)));
		subarrays.add(first);

		if (ctx.arrayDefComplexNPLus1Row().isEmpty()) {
			ArrayDef ar = new ArrayDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), first);
			ar.isArray = true;
			return ar;
		}

		ctx.arrayDefComplexNPLus1Row().forEach(a -> subarrays.add((ArrayList<Expression>) a.accept(this)));

		ArrayList<Expression> items = new ArrayList<Expression>(subarrays.size());
		for (ArrayList<Expression> sub : subarrays) {
			Expression fistitem = sub.get(0);
			if (sub.size() == 1) {
				items.add(fistitem);
			} else {
				ArrayDef ar = new ArrayDef(fistitem.getLine(), fistitem.getColumn(), sub);
				ar.isArray = true;
				items.add(ar);
			}
		}

		return new ArrayDefComplex(ctx.start.getLine(), ctx.start.getCharPositionInLine(), items);
	}

	/*
	 * @Override public Expression
	 * visitExprWithRefShortcut(ConcurnasParser.ExprWithRefShortcutContext ctx) {
	 * int line = ctx.start.getLine(); int col = ctx.start.getCharPositionInLine();
	 * 
	 * int refCnt = ctx.refCnt.size(); Expression ret; if(refCnt > 0) { ret= new
	 * AsyncRefRef(line, col, (Expression) ctx.atom().accept(this), refCnt); }else {
	 * ret = (Expression)ctx.expr_stmt_().accept(this); }
	 * 
	 * return ret; }
	 */

	@Override
	public ArrayList<Expression> visitArrayDefComplexNPLus1Row(ConcurnasParser.ArrayDefComplexNPLus1RowContext ctx) {
		ArrayList<Expression> ret = new ArrayList<Expression>();
		ctx.expr_stmt_().forEach(a -> ret.add((Expression) a.accept(this)));
		return ret;
	}

	@Override
	public MapDef visitMapDef(ConcurnasParser.MapDefContext ctx) {

		ArrayList<IsAMapElement> elements = new ArrayList<IsAMapElement>();
		ctx.mapDefElement().forEach(a -> elements.add((IsAMapElement) a.accept(this)));

		return new MapDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), elements);
	}

	@Override
	public IsAMapElement visitMapDefElement(ConcurnasParser.MapDefElementContext ctx) {
		Expression value = (Expression) ctx.value.accept(this);
		if (ctx.isDefault != null) {
			return new MapDefaultElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), value);
		} else {
			return new MapDefElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.key.accept(this), value);
		}
	}

	@Override
	public SuperOrThisConstructorInvoke visitSuperOrThisConstructorInvoke(ConcurnasParser.SuperOrThisConstructorInvokeContext ctx) {
		FuncInvokeArgs args = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);

		if (ctx.isthis != null) {
			return new ThisConstructorInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), args);
		} else {
			return new SuperConstructorInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), args);
		}
	}

	@Override
	public Expression visitConstructorInvoke(ConcurnasParser.ConstructorInvokeContext ctx){
		if(ctx.primNamedOrFuncType() != null) {
			Type tt = (Type)ctx.primNamedOrFuncType().accept(this);
			tt = applyMutators(tt, getMutators(ctx.refOrNullable()));
			
			return new New(ctx.start.getLine(), ctx.start.getCharPositionInLine(), tt , null, true);
		}
		return (Expression)visitChildren(ctx);
	}
	
	@Override
	public Expression visitNamedConstructor(ConcurnasParser.NamedConstructorContext ctx) {
		FuncRefArgs funcrefargs = null;
		FuncInvokeArgs funcinvokeargs = null;

		if (ctx.pureFuncInvokeArgs() != null) {
			funcinvokeargs = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);
		}

		Type tt = (Type)ctx.type().accept(this);
		/*ArrayList<Fourple<RefOrArryEnum, Object, Integer, Integer>>  mutators = getMutators(ctx.refOrNullable());
		tt = applyMutators(tt, mutators);*/
		
		New ret = new New(ctx.start.getLine(), ctx.start.getCharPositionInLine(), tt , funcinvokeargs, true);

		if (ctx.isConsRef != null) {
			if (ctx.funcRefArgs() != null) {
				funcrefargs = (FuncRefArgs) ctx.funcRefArgs().accept(this);
			}
			return new NamedConstructorRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ret, funcrefargs);
		} else if (ret.typeee.hasArrayLevels()) {// h1 = new MyClass[3](new MyClass(6))
			// incorrectly captured here, this is actually an array constructor - with
			// default value
			int levels = ret.typeee.getArrayLevels();
			ret.typeee.setArrayLevels(0);

			int line = ret.getLine();
			int col = ret.getColumn();

			ArrayList<Expression> arrayLevels = new ArrayList<Expression>();
			arrayLevels.add(new VarInt(line, col, levels));

			return new ArrayConstructor(line, col, ret.typeee, arrayLevels, funcinvokeargs == null? null:funcinvokeargs.asnames.get(0));
		}else {
			/*if(!TypeCheckUtils.hasRefLevels(tt)) {
				parserErrors.errors.add(new ErrorHolder(sourceName, ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Constructor parameters must be specified"));
			}*/
		}

		return ret;
	}

	public New visitNewreftypeOnOwn(ConcurnasParser.NewreftypeOnOwnContext ctx) {
		Type ret = (Type) ctx.typex.accept(this);

		FuncInvokeArgs funcinvokeargs = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);

		return new New(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Type) applyMutators(ret, getMutators(ctx.trefOrArrayRef())), funcinvokeargs, true);
	}

	@Override
	public ConstructorInvoke visitNamedActorConstructor(ConcurnasParser.NamedActorConstructorContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		FuncRefArgs funcrefargs = null;
		FuncInvokeArgs funcinvokeargs = null;

		if (ctx.pureFuncInvokeArgs() != null) {
			funcinvokeargs = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);
		}

		NamedType typeee = ScopeAndTypeChecker.const_typed_actor.copyTypeSpecific();
		typeee.setLine(line);
		typeee.setColumn(col);
		typeee.setGenTypes((Type) ctx.namedType_ExActor().accept(this));
		typeee.isDefaultActor = true;

		New ret = new New(line, col, typeee, funcinvokeargs, ctx.isNewDefiend != null);

		if (ctx.isConsRef != null) {
			if (ctx.funcRefArgs() != null) {
				funcrefargs = (FuncRefArgs) ctx.funcRefArgs().accept(this);
			}
			return new NamedConstructorRef(line, col, ret, funcrefargs);
		}

		return ret;
	}

	@Override
	public ArrayConstructor visitArrayConstructor(ConcurnasParser.ArrayConstructorContext ctx) {
		ArrayList<Expression> arraylevels = new ArrayList<Expression>();
		ctx.arconExprsSubsection().forEach(a -> arraylevels.addAll((ArrayList<Expression>) a.accept(this)));

		if (!ctx.nullEnd.isEmpty()) {
			for (org.antlr.v4.runtime.Token t : ctx.nullEnd) {
				arraylevels.add(null);
			}
		}

		List<ConcurnasParser.PrimNamedOrFuncTypeContext> pnorfs = ctx.primNamedOrFuncType();

		ConcurnasParser.PrimNamedOrFuncTypeContext first = pnorfs.get(0);
		Type type;
		if (pnorfs.size() == 1) {
			type = (Type) first.accept(this);
		} else {
			ArrayList<Type> multitype = new ArrayList<Type>(pnorfs.size());
			pnorfs.forEach(btc -> multitype.add((Type) btc.accept(this)));
			type = new MultiType(first.start.getLine(), first.start.getCharPositionInLine(), multitype);
		}

		Expression defaultValue = null != ctx.expr_stmt_tuple() ? (Expression) ctx.expr_stmt_tuple().accept(this) : null;

		return new ArrayConstructor(ctx.start.getLine(), ctx.start.getCharPositionInLine(), type, arraylevels, defaultValue);
	}

	/*
	 * @Override public Type
	 * visitPrimNamedOrFuncType(ConcurnasParser.PrimNamedOrFuncTypeContext ctx) {
	 * return (Type)visitChildren(ctx); }
	 */

	@Override
	public ArrayConstructor visitArrayConstructorPrimNoNew(ConcurnasParser.ArrayConstructorPrimNoNewContext ctx) {
		ArrayList<Expression> arraylevels = new ArrayList<Expression>();
		ctx.arconExprsSubsection().forEach(a -> arraylevels.addAll((ArrayList<Expression>) a.accept(this)));

		if (!ctx.nullEnd.isEmpty()) {
			for (org.antlr.v4.runtime.Token t : ctx.nullEnd) {
				arraylevels.add(null);
			}
		}

		Type type = (Type) ctx.primitiveType().accept(this);
		
		//type = applyMutators(type, getMutators(ctx.refOrNullable()));

		Expression defaultValue = null != ctx.expr_stmt_tuple() ? (Expression) ctx.expr_stmt_tuple().accept(this) : null;

		return new ArrayConstructor(ctx.start.getLine(), ctx.start.getCharPositionInLine(), type, arraylevels, defaultValue);
	}

	@Override
	public ArrayList<Expression> visitArconExprsSubsection(ConcurnasParser.ArconExprsSubsectionContext ctx) {
		ArrayList<Expression> arraylevels = new ArrayList<Expression>();
		ctx.expr_stmt().forEach(a -> arraylevels.add((Expression) a.accept(this)));

		if (!ctx.commaEnd.isEmpty()) {
			for (org.antlr.v4.runtime.Token t : ctx.commaEnd) {
				arraylevels.add(null);
			}
		}

		return arraylevels;
	}

	@Override
	public Expression visitClassNode(ConcurnasParser.ClassNodeContext ctx) {
		return new RefClass(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Type) ctx.type().accept(this));
	}

	@Override
	public Type visitRetTypeIncVoid(ConcurnasParser.RetTypeIncVoidContext ctx) {
		if (null != ctx.type()) {
			return (Type) ctx.type().accept(this);
		} else {
			return (Type) ScopeAndTypeChecker.const_void.copy();
		}
	}

	@Override
	public GrandLogicalOperatorEnum visitRelationalOperator(ConcurnasParser.RelationalOperatorContext ctx) {
		return GrandLogicalOperatorEnum.symToEnum.get(ctx.getText());
	}

	@Override
	public RefName visitOutNode(ConcurnasParser.OutNodeContext ctx) {
		return new RefName(ctx.start.getLine(), ctx.start.getCharPositionInLine(), "out");
	}

	@Override
	public Expression visitNestedNode(ConcurnasParser.NestedNodeContext ctx) {
		return (Expression) ctx.expr_stmt_tuple().accept(this);
	}

	@Override
	public Expression visitCopyExpr(ConcurnasParser.CopyExprContext ctx) {
		if(ctx.isCopy != null) {
			List<CopyExprItem> copyItems = null;
			
			if(ctx.hasCopier != null) {
				copyItems = ctx.copyExprItem().stream().map(a -> (CopyExprItem)a.accept(this)).collect(Collectors.toList());
			}
			
			List<String> modifiers = ctx.modifier.stream().map(a -> a.getText()).collect(Collectors.toList());
			
			return new CopyExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.expr_stmt_BelowDot().accept(this), copyItems, modifiers);
		}
		
		return (Expression)ctx.expr_stmt_BelowDot().accept(this);
	}

	
	@Override
	public CopyExprItem visitCopyExprItem(ConcurnasParser.CopyExprItemContext ctx) {
		if(ctx.ename != null) {
			//CopyExprAssign
			return new CopyExprAssign(ctx.ename.getText(), (Expression)ctx.expr_stmt().accept(this));
		}else if(ctx.incName != null) {
			return new CopyExprIncOnly( ctx.incName.getText() );
		}else if(!ctx.exclName.isEmpty()) {
			return new CopyExprExclOnly( ctx.exclName.stream().map(a -> a.getText()).collect(Collectors.toList()) );
		}else if(ctx.copyName != null) {
			//ctx.unchecked
			List<CopyExprItem> copyItems = ctx.copyExprItem().stream().map(a -> (CopyExprItem)a.accept(this)).collect(Collectors.toList());
			

			List<String> modifiers = ctx.modifier.stream().map(a -> a.getText()).collect(Collectors.toList());
			
			return new CopyExprNested(ctx.copyName.getLine(), ctx.copyName.getCharPositionInLine(), ctx.copyName.getText(), copyItems,modifiers);
		}else if(ctx.superCopy != null) {
			List<CopyExprItem> copyItems = ctx.copyExprItem().stream().map(a -> (CopyExprItem)a.accept(this)).collect(Collectors.toList());
			

			List<String> modifiers = ctx.modifier.stream().map(a -> a.getText()).collect(Collectors.toList());
			
			return new CopyExprSuper(ctx.superCopy.getLine(), ctx.superCopy.getCharPositionInLine(), copyItems, modifiers);
		}
		
		return null;//visitChildren(ctx);
	}
	
	@Override
	public Expression visitAsyncSpawnExpr(ConcurnasParser.AsyncSpawnExprContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		if (ctx.isAsync == null) {
			return (Expression) ctx.notNullAssertion().accept(this);
		} else {
			Block body = new Block(line, col);
			body.add(new LineHolder(line, col, new DuffAssign(line, col, (Expression) ctx.notNullAssertion().accept(this))));
			body.setShouldBePresevedOnStack(true);
			body.isolated = true;

			AsyncBlock ret;
			if (ctx.expr_stmt() == null) {
				ret = new AsyncBlock(line, col, body);
			} else {
				ret = new AsyncBlock(line, col, body, (Expression) ctx.expr_stmt().accept(this));
			}

			((Node) ret).setShouldBePresevedOnStack(true);

			return ret;
		}
	}

	@Override
	public Expression visitFuncInvokeExpr(ConcurnasParser.FuncInvokeExprContext ctx) {
		Expression lhsExpr = (Expression) ctx.expr_stmt_BelowDot().accept(this);

		ArrayList<Type> genTypes = new ArrayList<Type>();
		if (null != ctx.genTypeList()) {
			genTypes = (ArrayList<Type>) ctx.genTypeList().accept(this);
		}

		FuncInvokeArgs args = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);

		if (lhsExpr instanceof RefName) {
			return new FuncInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ((RefName) lhsExpr).name, args, genTypes);
		} else {
			return new FuncRefInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhsExpr, args);
		}
	}

	@Override
	public Expression visitFuncInvokeExprName(ConcurnasParser.FuncInvokeExprNameContext ctx) {
		String lhsExpr = ctx.NAME().getText();

		ArrayList<Type> genTypes = new ArrayList<Type>();
		if (null != ctx.genTypeList()) {
			genTypes = (ArrayList<Type>) ctx.genTypeList().accept(this);
		}

		FuncInvokeArgs args = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);

		return new FuncInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhsExpr, args, genTypes);
	}

	@Override
	public FuncRef visitFuncRefExpr(ConcurnasParser.FuncRefExprContext ctx) {
		FuncRefArgs args = ctx.funcRefArgs() == null ? null : (FuncRefArgs) ctx.funcRefArgs().accept(this);
		Expression functo = (Expression) ctx.expr_stmt_BelowDot().accept(this);

		FuncRef ret = new FuncRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), functo, args);

		if (null != ctx.genTypeList()) {
			ret.genTypes = (ArrayList<Type>) ctx.genTypeList().accept(this);
		}

		return ret;
	}

	@Override
	public Expression visitRefExpr(ConcurnasParser.RefExprContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		int refCnt = ctx.refCnt.size();

		Expression ret = new AsyncRefRef(line, col, (Expression) ctx.main.accept(this), refCnt);

		if (ctx.post != null) {
			ret = new DotOperator(line, col, ret, (Expression) ctx.post.accept(this));
		}

		return ret;
	}

	@Override
	public ArrayRef visitArrayRefExpr(ConcurnasParser.ArrayRefExprContext ctx) {
		// ArrayList<ArrayRefElement> elements = new ArrayList<ArrayRefElement>();
		// ctx.arrayRefElement().forEach(a -> elements.add((ArrayRefElement)
		// a.accept(this)));

		ArrayRefLevelElementsHolder arrayLevelElements = new ArrayRefLevelElementsHolder();

		boolean forceArrayConst = false;
		for (ConcurnasParser.ArrayRefElementsContext a : ctx.arrayRefElements()) {
			Pair<Boolean, ArrayList<ArrayRefElement>> nullAndLevels = (Pair<Boolean, ArrayList<ArrayRefElement>>) a.accept(this);
			ArrayList<ArrayRefElement> level = nullAndLevels.getB();
			Integer trails = level.get(level.size() - 1).trailingCommas;
			if (null != trails && trails >= 2) {
				forceArrayConst = true;
			}
			arrayLevelElements.add(nullAndLevels.getA(), level);
		}

		Expression lhs = (Expression) ctx.expr_stmt_BelowDot().accept(this);
		int refcnt = ctx.refCnt.size();
		if (refcnt > 0) {
			lhs = new AsyncRefRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhs, refcnt);
		}

		ArrayRef ret = new ArrayRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhs, arrayLevelElements);

		if (!ctx.extraEmptyBracks.isEmpty()) {
			ret.arrayConstructorExtraEmptyBracks = ctx.extraEmptyBracks.size();
		} else if (forceArrayConst) {
			ret.arrayConstructorExtraEmptyBracks = 0;
		}

		return ret;
	}

	@Override
	public Pair<Boolean, ArrayList<ArrayRefElement>> visitArrayRefElements(ConcurnasParser.ArrayRefElementsContext ctx) {
		ArrayList<ArrayRefElement> elements = new ArrayList<ArrayRefElement>();
		ctx.arrayRefElement().forEach(a -> elements.add((ArrayRefElement) a.accept(this)));

		if (!ctx.trailcomma.isEmpty()) {
			elements.get(elements.size() - 1).trailingCommas = ctx.trailcomma.size();
		}

		boolean nullsafe = ctx.nullSafe != null;
		
		return new Pair<>(nullsafe, elements);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public Node visitExprListShortcut(ConcurnasParser.ExprListShortcutContext ctx) {
		if (ctx.a1 != null) {
			Type type = (Type) ctx.atype1.accept(this);

			AssignStyleEnum assignStyle = null;
			Expression assignment = null;

			if (ctx.assignStyle() != null) {
				assignStyle = (AssignStyleEnum) ctx.assignStyle().accept(this);
				assignment = (Expression) ctx.arhsExpr.accept(this);
			}

			return new AssignNew(null, ctx.start.getLine(), ctx.start.getCharPositionInLine(), false, false, ((RefName) ctx.a1.accept(this)).name, null, type, assignStyle, assignment);
		} else {

			ArrayList<Expression> exprs = new ArrayList<Expression>();
			exprs.add((RefName) ctx.e1.accept(this));
			exprs.add((RefName) ctx.e2.accept(this));

			ctx.expr_stmt_().forEach(a -> exprs.add((Expression) a.accept(this)));

			ExpressionList ret = new ExpressionList(ctx.start.getLine(), ctx.start.getCharPositionInLine(), exprs);
			ret.couldBeAnAssignmentDecl = true;
			return ret;
		}
	}

	@Override
	public Type visitMustBeArrayType(ConcurnasParser.MustBeArrayTypeContext ctx) {
		Type ret;
		if (ctx.primitiveType() != null) {
			ret = (Type) ctx.primitiveType().accept(this);
		} else if (ctx.namedType() != null) {
			ret = (Type) ctx.namedType().accept(this);
		} else {
			ret = (Type) ctx.funcType().accept(this);
		}
		return applyMutators(ret, getMutators(ctx.trefOrArrayRef()));
	}

	/*
	 * @Override public Expression visitExpr_stmt(ConcurnasParser.Expr_stmtContext
	 * ctx) {
	 * 
	 * 
	 * }
	 */

	@Override
	public Expression visitExpr_list(ConcurnasParser.Expr_listContext ctx) {
		if (ctx.lambdadef() != null) {
			return (Expression) ctx.lambdadef().accept(this);
		}else if(ctx.anonLambdadef() != null) {
			return (Expression) ctx.anonLambdadef().accept(this);
		}else if(ctx.lambdadefOneLine() != null) {
			return (Expression) ctx.lambdadefOneLine().accept(this);
		}/*else if(ctx.block_async() != null) {
			return (Expression) ctx.block_async().accept(this);
		}*/

		/*
		 * if(ctx.for_list_comprehension() != null){ return
		 * (Expression)ctx.for_list_comprehension().accept(this); }
		 */

		if (ctx.children.size() == 1) {// most common case
			return (Expression) ctx.expr_stmt_().get(0).accept(this);
		} else {
			ArrayList<Expression> exprs = new ArrayList<Expression>();

			ctx.expr_stmt_().forEach(a -> exprs.add((Expression) a.accept(this)));

			return new ExpressionList(ctx.start.getLine(), ctx.start.getCharPositionInLine(), exprs);
		}
	}

	@Override
	public Expression visitFor_list_comprehension(ConcurnasParser.For_list_comprehensionContext ctx) {
		if (ctx.flc_forStmt_().isEmpty()) {
			return (Expression) ctx.mainExpr.accept(this);
		} else {// transform into a for compri
				// (if_expr | block) forblockvariant NAME type? 'in' expr_stmt ('if' expr_stmt)?
				// ;
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();

			Block mainBlock = new Block(line, col, (Expression) ctx.mainExpr.accept(this));

			if (ctx.condexpr != null) {
				Expression ifcond = (Expression) ctx.condexpr.accept(this);
				ArrayList<ElifUnit> elifunits = new ArrayList<ElifUnit>();
				IfStatement gate = new IfStatement(line, col, ifcond, mainBlock, elifunits, null);
				mainBlock = new Block(line, col, gate);
			}

			List<ConcurnasParser.Flc_forStmt_Context> forItems = ctx.flc_forStmt_();
			int itmcnt = forItems.size();

			Block blcokAddTo = null;
			Expression toreturn = null;
			for (int n = 0; n < itmcnt; n++) {
				boolean isLast = itmcnt - 1 == n;

				FLCForStmt flcsttm = (FLCForStmt) forItems.get(n).accept(this);

				int linex = flcsttm.line;
				int colx = flcsttm.col;

				ForBlock component;
				if(flcsttm.assignTup != null) {
					component = new ForBlock(linex, colx, flcsttm.assignTup, flcsttm.expr, isLast ? mainBlock : new Block(linex, colx), flcsttm.pfv);
				}else {
					component = new ForBlock(linex, colx, flcsttm.localVarName, flcsttm.localVarType, flcsttm.expr, isLast ? mainBlock : new Block(linex, colx), flcsttm.pfv);
				}
				
				component.setShouldBePresevedOnStack(true);
				component.isListcompri = true;

				if (flcsttm.pfv != null && ctx.condexpr != null) {// it's an error to have a if condition and use a parfor or parforsync (since itoperates on every element)
					component.flagErrorListCompri = true;
				}

				Expression componentE = component;
				if(flcsttm.pfv != null) {
					componentE = convertForIfParfor(line, col, flcsttm.pfv, component);
				}
				
				if (null == toreturn) {
					toreturn = componentE;
				} else {
					blcokAddTo.add((Line)componentE);
				}

				blcokAddTo = component.block;
			}

			return toreturn;
		}
	}

	private static class FLCForStmt {
		public int line;
		public int col;
		public ForBlockVariant pfv;
		public String localVarName;
		public Type localVarType;
		public Expression expr;
		public AssignTupleDeref assignTup;

		public FLCForStmt(int line, int col, ForBlockVariant pfv, AssignTupleDeref assignTup, Expression expr) {
			this.line = line;
			this.col = col;
			this.pfv = pfv;
			this.assignTup = assignTup;
			this.expr = expr;
			
		}
		public FLCForStmt(int line, int col, ForBlockVariant pfv, String localVarName, Type localVarType, Expression expr) {
			this.line = line;
			this.col = col;
			this.pfv = pfv;
			this.localVarName = localVarName;
			this.localVarType = localVarType;
			this.expr = expr;
		}
	}

	@Override
	public FLCForStmt visitFlc_forStmt_(ConcurnasParser.Flc_forStmt_Context ctx) {
		ForBlockVariant pfv = (ForBlockVariant) ctx.forblockvariant().accept(this);
		
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		Expression expr = (Expression) ctx.expr.accept(this);
		
		if(!ctx.forVarTupleOrNothing().isEmpty()) {
			AssignTupleDeref assignTup =  makeAssignTupleDerefForLoop(line, col, ctx.forVarTupleOrNothing());
			return new FLCForStmt(line, col, pfv, assignTup, expr);
		}else {
			String localVarName = ctx.localVarName.getText();
			Type localVarType = ctx.localVarType == null ? null : (Type) ctx.localVarType.accept(this);
			return new FLCForStmt(line, col, pfv, localVarName, localVarType, expr);
		}
	}

	@Override
	public Expression visitIf_expr(ConcurnasParser.If_exprContext ctx) {
		if (ctx.test != null) {
			return IfStatement.makeFromTwoExprs(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (Expression) ctx.op1.accept(this), (Expression) ctx.op2.accept(this), (Expression) ctx.test.accept(this));
		} else {
			return (Expression) ctx.op1.accept(this);
		}
	}

	@Override
	public Expression visitExpr_stmt_or(ConcurnasParser.Expr_stmt_orContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<RedirectableExpression> elements = new ArrayList<RedirectableExpression>(0);// mostly

		ctx.ors.forEach(a -> elements.add(new RedirectableExpression((Expression) a.accept(this))));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new OrExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}

	}

	@Override
	public Expression visitExpr_stmt_and(ConcurnasParser.Expr_stmt_andContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<RedirectableExpression> elements = new ArrayList<RedirectableExpression>(0);// mostly
		ctx.ands.forEach(a -> elements.add(new RedirectableExpression((Expression) a.accept(this))));
		if (elements.isEmpty()) {
			return head;
		} else {
			return new AndExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public Expression visitBitwise_or(ConcurnasParser.Bitwise_orContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<RedirectableExpression> elements = new ArrayList<RedirectableExpression>(0);
		ctx.ands.forEach(a -> elements.add(new RedirectableExpression((Expression) a.accept(this))));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new BitwiseOperation(ctx.start.getLine(), ctx.start.getCharPositionInLine(), BitwiseOperationEnum.OR, head, elements);
		}
	}

	@Override
	public Expression visitBitwise_xor(ConcurnasParser.Bitwise_xorContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<RedirectableExpression> elements = new ArrayList<RedirectableExpression>(0);
		ctx.ands.forEach(a -> elements.add(new RedirectableExpression((Expression) a.accept(this))));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new BitwiseOperation(ctx.start.getLine(), ctx.start.getCharPositionInLine(), BitwiseOperationEnum.XOR, head, elements);
		}
	}

	@Override
	public Expression visitBitwise_and(ConcurnasParser.Bitwise_andContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<RedirectableExpression> elements = new ArrayList<RedirectableExpression>(0);
		ctx.ands.forEach(a -> elements.add(new RedirectableExpression((Expression) a.accept(this))));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new BitwiseOperation(ctx.start.getLine(), ctx.start.getCharPositionInLine(), BitwiseOperationEnum.AND, head, elements);
		}
	}

	@Override
	public Expression visitExpr_stmt_BelowEQ(ConcurnasParser.Expr_stmt_BelowEQContext ctx) {
		Expression head = (Expression) ctx.head.accept(this);

		ArrayList<GrandLogicalElement> elements = new ArrayList<GrandLogicalElement>(0);
		ctx.eqAndExpression_().forEach(a -> elements.add((GrandLogicalElement) a.accept(this)));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new EqReExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public GrandLogicalElement visitEqAndExpression_(ConcurnasParser.EqAndExpression_Context ctx) {
		GrandLogicalOperatorEnum compOp2 = (GrandLogicalOperatorEnum) ctx.equalityOperator().accept(this);
		Expression e2 = (Expression) ctx.instanceof_expr().accept(this);

		return new GrandLogicalElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), compOp2, e2);
	}

	@Override
	public Expression visitInstanceof_expr(ConcurnasParser.Instanceof_exprContext ctx) {
		Expression head = (Expression) ctx.castExpr().accept(this);

		ArrayList<Type> typees = new ArrayList<Type>();
		ctx.type().forEach(a -> typees.add((Type) a.accept(this)));

		if (!typees.isEmpty()) {
			Is ret = new Is(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, typees, ctx.invert != null);
			// ret.isas = ctx.isas != null;
			return ret;
		} else {
			return head;
		}
	}

	@Override
	public Expression visitCastExpr(ConcurnasParser.CastExprContext ctx) {
		Expression head = (Expression) ctx.lTGTExpr().accept(this);

		if (!ctx.type().isEmpty()) {
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			for (ConcurnasParser.TypeContext tc : ctx.type()) {
				Type tt = (Type) tc.accept(this);
				head = new CastExpression(line, col, tt, head);
			}

			return head;
		} else {
			return head;
		}
	}

	@Override
	public Expression visitLTGTExpr(ConcurnasParser.LTGTExprContext ctx) {
		Expression head = (Expression) ctx.shiftExpr().accept(this);

		ArrayList<GrandLogicalElement> elements = new ArrayList<GrandLogicalElement>(1);
		ctx.relOpAndExpression_().forEach(a -> elements.add((GrandLogicalElement) a.accept(this)));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new EqReExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public GrandLogicalElement visitRelOpAndExpression_(ConcurnasParser.RelOpAndExpression_Context ctx) {
		GrandLogicalOperatorEnum compOp2 = (GrandLogicalOperatorEnum) ctx.relationalOperator().accept(this);
		Expression e2 = (Expression) ctx.shiftExpr().accept(this);
		return new GrandLogicalElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), compOp2, e2);
	}

	@Override
	public Expression visitComppound_str_concat(ConcurnasParser.Comppound_str_concatContext ctx) {
		Expression head = (Expression) ctx.compound_stmt().accept(this);

		ArrayList<AddMinusExpressionElement> elements = new ArrayList<AddMinusExpressionElement>(1);// mostly
		ctx.additiveOp_().forEach(a -> elements.add((AddMinusExpressionElement) a.accept(this)));

		String hh = "sdfsdf";
		String res = "" + new StringBuilder(hh).reverse();

		if (elements.isEmpty()) {
			return head;
		} else {
			return new Additive(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public Expression visitAdditiveExpr(ConcurnasParser.AdditiveExprContext ctx) {
		Expression head = (Expression) ctx.divisiveExpr().accept(this);

		ArrayList<AddMinusExpressionElement> elements = new ArrayList<AddMinusExpressionElement>(1);// mostly
		ctx.additiveOp_().forEach(a -> elements.add((AddMinusExpressionElement) a.accept(this)));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new Additive(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public AddMinusExpressionElement visitAdditiveOp_(ConcurnasParser.AdditiveOp_Context ctx) {
		boolean isPlus = ctx.op.getText().equals("+");
		return new AddMinusExpressionElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), isPlus, (Expression) ctx.divisiveExpr().accept(this));
	}

	@Override
	public Expression visitShiftExpr(ConcurnasParser.ShiftExprContext ctx) {
		Expression head = (Expression) ctx.additiveExpr().accept(this);

		ArrayList<ShiftElement> elements = new ArrayList<ShiftElement>(1);// mostly
		ctx.shiftExprOp_().forEach(a -> elements.add((ShiftElement) a.accept(this)));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new ShiftExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public ShiftElement visitShiftExprOp_(ConcurnasParser.ShiftExprOp_Context ctx) {
		// ShiftOperatorEnum em =
		// ShiftOperatorEnum.symToEnum.get(ctx.shiftOperator.getText());

		ShiftOperatorEnum em;
		if (ctx.lshift != null) {
			em = ShiftOperatorEnum.LS;
		} else if (ctx.rshift != null) {
			em = ShiftOperatorEnum.RS;
		} else {
			em = ShiftOperatorEnum.URS;
		}

		return new ShiftElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), em, (Expression) ctx.additiveExpr().accept(this));
	}

	@Override
	public Expression visitDivisiveExpr(ConcurnasParser.DivisiveExprContext ctx) {
		Expression head = (Expression) ctx.powExpr().accept(this);

		ArrayList<MulerElement> elements = new ArrayList<MulerElement>(1);// mostly
		ctx.divisiveExprOP_().forEach(a -> elements.add((MulerElement) a.accept(this)));

		if (elements.isEmpty()) {
			return head;
		} else {
			return new MulerExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, elements);
		}
	}

	@Override
	public MulerElement visitDivisiveExprOP_(ConcurnasParser.DivisiveExprOP_Context ctx) {
		MulerExprEnum isPlus = MulerExprEnum.MOD;
		String optxt = ctx.op.getText();
		switch (optxt) {
		case "*":
			isPlus = MulerExprEnum.MUL;
			break;
		case "/":
			isPlus = MulerExprEnum.DIV;
			break;
		}

		return new MulerElement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), isPlus, (Expression) ctx.powExpr().accept(this));
	}

	@Override
	public Expression visitPowExpr(ConcurnasParser.PowExprContext ctx) {
		ArrayList<Expression> powExpressions = new ArrayList<Expression>();
		ctx.rhs.forEach(a -> powExpressions.add((Expression) a.accept(this)));

		Expression head = (Expression) ctx.lhs.accept(this);

		if (powExpressions.isEmpty()) {
			return head;
		} else {
			powExpressions.add(0, head);
			Collections.reverse(powExpressions);
			Expression rhsExpression = null;
			for (Expression expr : powExpressions) {
				if (rhsExpression == null) {
					rhsExpression = expr;
				} else {
					rhsExpression = new PowOperator(expr.getLine(), expr.getColumn(), expr, rhsExpression);
				}
			}
			return rhsExpression;
		}
	}

	@Override
	public Expression visitPrefixExpr(ConcurnasParser.PrefixExprContext ctx) {
		Expression rhs = (Expression) ctx.postfixExpr().accept(this);
		if (ctx.prefixOp == null) {
			return rhs;
		} else {
			String opStr = ctx.prefixOp.getText();

			FactorPrefixEnum op = null;
			switch (opStr) {
			case "++":
				op = FactorPrefixEnum.PLUSPLUS;
				break;
			case "+":
				op = FactorPrefixEnum.PLUS;
				break;
			case "--":
				op = FactorPrefixEnum.MINUSMINUS;
				break;
			case "-":
				op = FactorPrefixEnum.NEG;
				break;
			case "comp":
				op = FactorPrefixEnum.COMP;
				break;
			}

			return new PrefixOp(ctx.start.getLine(), ctx.start.getCharPositionInLine(), op, rhs);
		}
	}

	@Override
	public Expression visitPostfixExpr(ConcurnasParser.PostfixExprContext ctx) {
		Expression rhs = (Expression) ctx.sizeOfExpr().accept(this);
		if (null == ctx.postfixOp) {
			return rhs;
		} else {
			FactorPostFixEnum op = ctx.postfixOp.getText().equals("++") ? FactorPostFixEnum.PLUSPLUS : FactorPostFixEnum.MINUSMINUS;
			return new PostfixOp(ctx.start.getLine(), ctx.start.getCharPositionInLine(), op, rhs);
		}
	}

	@Override
	public Expression visitNotNullAssertion(ConcurnasParser.NotNullAssertionContext ctx) {
		Expression rhs = (Expression) ctx.elvisOperator().accept(this);
		if (null == ctx.nna) {
			return rhs;
		}else {
			return new NotNullAssertion(ctx.start.getLine(), ctx.start.getCharPositionInLine(), rhs);
		}
	}
	
	@Override
	public Expression visitNotNullAssertion2(ConcurnasParser.NotNullAssertion2Context ctx) {
		Expression rhs = (Expression) ctx.atom().accept(this);
		if (null == ctx.nna) {
			return rhs;
		}else {
			return new NotNullAssertion(ctx.start.getLine(), ctx.start.getCharPositionInLine(), rhs);
		}
	}
	
	@Override
	public Expression visitElvisOperator(ConcurnasParser.ElvisOperatorContext ctx) {
		Expression lhsExpr = (Expression) ctx.lhsExpr.accept(this);
		if (null == ctx.elsExpr) {
			return lhsExpr;
		}else {
			return new ElvisOperator(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhsExpr, (Expression) ctx.elsExpr.accept(this));
		}
	}
	
	@Override
	public Expression visitNotExpr(ConcurnasParser.NotExprContext ctx) {
		Expression rhs = (Expression) ctx.containsExpr().accept(this);
		if (null == ctx.isnot) {
			return rhs;
		} else {
			return new NotExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), rhs);
		}

	}

	@Override
	public Expression visitContainsExpr(ConcurnasParser.ContainsExprContext ctx) {
		Expression lhs = (Expression) ctx.lhs.accept(this);
		if (ctx.rhs == null) {
			return lhs;
		} else {
			boolean invert = ctx.invert != null;
			return new InExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), lhs, (Expression) ctx.rhs.accept(this), invert);

		}
	}

	@Override
	public Expression visitSizeOfExpr(ConcurnasParser.SizeOfExprContext ctx) {
		Expression head = (Expression) ctx.asyncSpawnExpr().accept(this);

		if (ctx.sizeof == null) {
			return head;
		} else {
			String variant = ctx.variant == null ? null : (String) ctx.variant.accept(this);

			return new SizeofStatement(ctx.start.getLine(), ctx.start.getCharPositionInLine(), head, variant);
		}
	}

	@Override
	public Expression visitDotOperatorExpr(ConcurnasParser.DotOperatorExprContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		Expression head = null;
		ArrayList<Expression> elements = new ArrayList<Expression>();
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>();
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>();
		ArrayList<Boolean> safeCall = new ArrayList<Boolean>();
		int n = 0;
		for (ConcurnasParser.CopyExprContext itm : ctx.copyExpr()) {
			Expression expr = (Expression) itm.accept(this);
			if (head == null) {
				head = expr;
			} else {
				elements.add(expr);
				String dddop = ctx.dotOpArg(n++).getText();
				isDirectAccess.add(dddop.equals("\\."));
				returnCalledOn.add(dddop.equals(".."));
				safeCall.add(dddop.equals("?."));
			}
		}
		Expression ret;

		if (elements.size() == 0) {
			ret = head;
		} else {
			ret = new DotOperator(line, col, head, elements, isDirectAccess, returnCalledOn, safeCall);
		}

		if (ctx.address != null) {
			ret = new PointerAddress(line, col, ret);
		} else if (!ctx.pntUnrefCnt.isEmpty() || !ctx.pntUnrefCnt2.isEmpty()) {
			ret = new PointerUnref(line, col, ctx.pntUnrefCnt.size() + (2 * ctx.pntUnrefCnt2.size()), ret);
		}

		return ret;
	}

	@Override
	public RefQualifiedGenericNamedType visitRefQualifiedGeneric(ConcurnasParser.RefQualifiedGenericContext ctx) {
		return new RefQualifiedGenericNamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.NAME().getText(), (ArrayList<Type>) ctx.genTypeList().accept(this));
	}

	@Override
	public RefQualifiedGenericNamedType visitRefQualifiedGenericActor(ConcurnasParser.RefQualifiedGenericActorContext ctx) {
		String name = (String) ctx.dotted_name().accept(this);
		if (ctx.genTypeList() == null) {
			return new RefQualifiedGenericNamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), name, true);
		} else {
			return new RefQualifiedGenericNamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), name, (ArrayList<Type>) ctx.genTypeList().accept(this), true);
		}
	}

	@Override
	public Expression visitVectorize(ConcurnasParser.VectorizeContext ctx) {
		if (ctx.passthrough != null) {
			return (Expression) ctx.passthrough.accept(this);
		} else {
			Expression ret = (Expression) ctx.primary.accept(this);

			for (ConcurnasParser.Vectorize_elementContext eleContx : ctx.vectorize_element()) {
				VecElement ele = (VecElement) eleContx.accept(this);

				if (ele.isFuncRefOrFuncInvoke) {
					if (ele.pureFuncInvokeArgs != null) {
						ret = new VectorizedFuncInvoke(ctx.start.getLine(), ctx.start.getCharPositionInLine(), (ele.afterVecExpr).name, ele.pureFuncInvokeArgs, ele.genTypes, ret, ele.doubleDot, ele.nullsafe);
					} else if (null != ele.funcRefArgs) {
						ret = new VectorizedFuncRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ele.afterVecExpr, ele.funcRefArgs, ele.genTypes, ret, ele.doubleDot, ele.nullsafe);
					} else {// vectorizedFieldRef
						ret = new VectorizedFieldRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ele.afterVecExpr, ret, ele.doubleDot, ele.nullsafe);
					}
				} else if (ele.arrayRefElementsHolder != null) {
					if(ele.nullsafe) {
						parserErrors.errors.add(new ErrorHolder(sourceName, ctx.start.getLine(), ctx.start.getCharPositionInLine(), "null safe vectorization amy not be applied to array references"));
					}
					
					ret = new VectorizedArrayRef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ret, ele.arrayRefElementsHolder, ele.doubleDot, ele.nullsafe);
				} else if (null != ele.constru) {
					ret = new VectorizedNew(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ret, ele.constru, ele.doubleDot, ele.nullsafe);
				} else {
					if(ele.nullsafe) {
						parserErrors.errors.add(new ErrorHolder(sourceName, ctx.start.getLine(), ctx.start.getCharPositionInLine(), "null safe vectorization amy not be applied to element"));
					}
					
					ret = new Vectorized(ctx.start.getLine(), ctx.start.getCharPositionInLine(), ret, ele.doubleDot, ele.nullsafe);
				}

			}

			return ret;
		}
	}

	private static class VecElement {
		public final boolean doubleDot;
		public final boolean nullsafe;

		public VecElement(final boolean doubleDot, final boolean nullsafe) {
			this.doubleDot = doubleDot;
			this.nullsafe = nullsafe;
		}

		public boolean isFuncRefOrFuncInvoke;
		public ArrayList<Type> genTypes;
		public FuncInvokeArgs pureFuncInvokeArgs;
		public FuncRefArgs funcRefArgs;
		public Expression constru;
		public RefName afterVecExpr;
		public ArrayRefLevelElementsHolder arrayRefElementsHolder;
	}

	@Override
	public VecElement visitVectorize_element(ConcurnasParser.Vectorize_elementContext ctx) {
		VecElement ret = new VecElement(ctx.doubledot != null, ctx.nullsafe != null);

		if (ctx.afterVecExpr != null) {
			ret.isFuncRefOrFuncInvoke = true;
			if (ctx.genTypeList() != null) {
				ret.genTypes = (ArrayList<Type>) ctx.genTypeList().accept(this);
			}

			ret.afterVecExpr = (RefName) ctx.afterVecExpr.accept(this);

			if (ctx.pureFuncInvokeArgs() != null) {
				ret.pureFuncInvokeArgs = (FuncInvokeArgs) ctx.pureFuncInvokeArgs().accept(this);
			} else {
				if (ctx.funcRefArgs() != null) {
					ret.funcRefArgs = (FuncRefArgs) ctx.funcRefArgs().accept(this);
				}
			}
		} else if (!ctx.arrayRefElements().isEmpty()) {
			ArrayRefLevelElementsHolder arrayRefElementsHolder = new ArrayRefLevelElementsHolder();

			boolean forceArrayConst = false;
			for (ConcurnasParser.ArrayRefElementsContext a : ctx.arrayRefElements()) {
				Pair<Boolean, ArrayList<ArrayRefElement>> nullAndLevels = (Pair<Boolean, ArrayList<ArrayRefElement>>) a.accept(this);
				ArrayList<ArrayRefElement> level = nullAndLevels.getB();
				Integer trails = level.get(level.size() - 1).trailingCommas;
				if (null != trails && trails >= 2) {
					forceArrayConst = true;
				}
				arrayRefElementsHolder.add(nullAndLevels.getA(), level);
			}

			ret.arrayRefElementsHolder = arrayRefElementsHolder;
		} else if (null != ctx.constru) {
			ret.constru = (Expression) ctx.constru.accept(this);
		}

		return ret;
	}

	public static void sdfsdf() {
		Integer[] thing = null;
				
		Integer res = thing[0];
	}
	
	
	@Override
	public Type visitBareTypeParamTuple(ConcurnasParser.BareTypeParamTupleContext ctx) {
		Type ret;
		if (ctx.namedType() != null) {
			ret = (Type) ctx.namedType().accept(this);
		} else if (ctx.funcType() != null) {
			ret = (Type) ctx.funcType().accept(this);
		} else if (ctx.tupleType() != null) {
			ret = (Type) ctx.tupleType().accept(this);
		} else {
			ret = (Type) ctx.primitiveType().accept(this);
			if (ctx.pointerQualifier() != null) {
				int pp = (int) ctx.pointerQualifier().accept(this);
				ret.setPointer(pp);
			}

			return ret;
		}

		return ret;
	}
	

	@Override
	public Integer visitPointerQualifier(ConcurnasParser.PointerQualifierContext ctx) {
		return ctx.cnt.size() + (ctx.cnt2.size() * 2);
	}

	@Override
	public Type visitPrimNamedOrFuncType(ConcurnasParser.PrimNamedOrFuncTypeContext ctx) {
		Type tt;
		if (ctx.primitiveType() != null) {
			tt = (Type) ctx.primitiveType().accept(this);
			if (ctx.pointerQualifier() != null) {
				tt.setPointer((int) ctx.pointerQualifier().accept(this));
			}

		} else {
			if(null != ctx.namedType()) {
				tt = (Type)ctx.namedType().accept(this);
			}else if(null != ctx.funcType()) {
				tt = (Type)ctx.funcType().accept(this);
			}else{// if(null != ctx.tupleType()) {
				tt = (Type)ctx.tupleType().accept(this);
			}
		}
		
		return applyMutators(tt, getMutators(ctx.refOrNullable()));
	}

	@Override
	public Expression visitExpr_stmt_tuple(ConcurnasParser.Expr_stmt_tupleContext ctx) {
		int exprSize = ctx.expr_stmt().size();
		if (exprSize == 1) {
			return (Expression) ctx.expr_stmt().get(0).accept(this);
		} else {
			ArrayList<Expression> tupleElements = new ArrayList<Expression>(exprSize);
			ctx.expr_stmt().forEach(a -> tupleElements.add((Expression) a.accept(this)));

			return new TupleExpression(ctx.start.getLine(), ctx.start.getCharPositionInLine(), tupleElements);
		}
	}

	@Override
	public Type visitTupleType(ConcurnasParser.TupleTypeContext ctx) {
		ArrayList<Type> genTypes = new ArrayList<Type>();

		ctx.bareButTuple().forEach(a -> genTypes.add((Type) a.accept(this)));

		ArrayList<Pair<String, ArrayList<Type>>> nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();
		return new NamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Tuple" + genTypes.size(), genTypes, nestorSegments);
	}
	
	@Override
	public Type visitBareButTuple(ConcurnasParser.BareButTupleContext ctx) {
		Type ret;
		if(ctx.primitiveType() != null) {
			ret = (Type)ctx.primitiveType().accept(this);
		}else if(ctx.namedType() != null) {
			ret = (Type)ctx.namedType().accept(this);
		}else {//if(ctx.funcType() != null) {
			ret = (Type)ctx.funcType().accept(this);
		}
		
		return applyMutators(ret, getMutators(ctx.trefOrArrayRef()));
	}
	

	@Override
	public AnonLambdaDef visitAnonLambdadef(ConcurnasParser.AnonLambdadefContext ctx) {
		ArrayList<String> paramNames = new ArrayList<String>();
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		
		
		if(!ctx.typeAnonParam().isEmpty()) {
			for( ConcurnasParser.TypeAnonParamContext anonCtxt : ctx.typeAnonParam() ) {
				Pair<String, Type> anonParam = (Pair<String, Type>)anonCtxt.accept(this);
				paramNames.add(anonParam.getA());
				paramTypes.add(anonParam.getB());
				
			}
		}else {
			ctx.NAME().forEach(a -> paramNames.add(a.getText()));
		}
		
		
		Type retType = null;
		if(ctx.retType != null) {
			retType = (Type)ctx.retType.accept(this);
		}
		

		Block body = (Block)ctx.single_line_block().accept(this);
		
		return new AnonLambdaDef(ctx.start.getLine(), ctx.start.getCharPositionInLine(), paramNames, body, paramTypes, retType);
	}

	@Override
	public Pair<String, Type> visitTypeAnonParam(ConcurnasParser.TypeAnonParamContext ctx) {
		return new Pair<String, Type>(ctx.NAME().getText(), ctx.type() != null?(Type)ctx.type().accept(this) : null);
	}

	
	/////types/////////
	
	

	@Override
	public Type visitType(ConcurnasParser.TypeContext ctx) {
		Type ret;

		List<ConcurnasParser.BareTypeParamTupleContext> bts = ctx.bareTypeParamTuple();

		if (bts.size() == 1) {
			ret = (Type) ctx.bareTypeParamTuple().get(0).accept(this);
		} else {
			ArrayList<Type> multitype = new ArrayList<Type>(bts.size());
			bts.forEach(btc -> multitype.add((Type) btc.accept(this)));

			ret = new MultiType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), multitype);
		}

		return applyMutators(ret, getMutators(ctx.trefOrArrayRef()));
	}
	

	@Override
	public Type visitTypeNoNTTuple(ConcurnasParser.TypeNoNTTupleContext ctx) {
		Type ret;

		List<ConcurnasParser.BareTypeParamTupleNoNTContext> bts = ctx.bareTypeParamTupleNoNT();

		if (bts.size() == 1) {
			ret = (Type) ctx.bareTypeParamTupleNoNT().get(0).accept(this);
		} else {
			ArrayList<Type> multitype = new ArrayList<Type>(bts.size());
			bts.forEach(btc -> multitype.add((Type) btc.accept(this)));

			ret = new MultiType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), multitype);
		}

		return applyMutators(ret, getMutators(ctx.trefOrArrayRef()));
	}

	@Override
	public Type visitBareTypeParamTupleNoNT(ConcurnasParser.BareTypeParamTupleNoNTContext ctx) {
		Type ret;
		if (ctx.namedType() != null) {
			ret = (Type) ctx.namedType().accept(this);
		} else if (ctx.funcType() != null) {
			ret = (Type) ctx.funcType().accept(this);
		} else if (ctx.tupleTypeNoNT() != null) {
			ret = (Type) ctx.tupleTypeNoNT().accept(this);
		} else {
			ret = (Type) ctx.primitiveType().accept(this);
			if (ctx.pointerQualifier() != null) {
				int pp = (int) ctx.pointerQualifier().accept(this);
				ret.setPointer(pp);
			}
		}
		
		return ret;
	}

	@Override
	public Type visitTupleTypeNoNT(ConcurnasParser.TupleTypeNoNTContext ctx) {
		ArrayList<Type> genTypes = new ArrayList<Type>();

		ctx.bareButTupleNoNT().forEach(a -> genTypes.add((Type) a.accept(this)));

		ArrayList<Pair<String, ArrayList<Type>>> nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();
		return new NamedType(ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Tuple" + genTypes.size(), genTypes, nestorSegments);
	}

	@Override
	public ObjectProvider visitObjectProvider(ConcurnasParser.ObjectProviderContext ctx) {
		
		@SuppressWarnings("unchecked")
		ArrayList<Pair<String, NamedType>> classGenricList = ctx.genericQualiList() == null ? new ArrayList<Pair<String, NamedType>>() : (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this);
		
		return new ObjectProvider(ctx.start.getLine(), ctx.start.getCharPositionInLine(), 
				ctx.pppNoInject() == null ? null: (AccessModifier)ctx.pppNoInject().accept(this),
				ctx.providerName.getText(), 
				null == ctx.objectProviderArgs() ? null:(ClassDefArgs)ctx.objectProviderArgs().accept(this), 
				ctx.trans != null, 
				ctx.shared != null,
				(ObjectProviderBlock)ctx.objectProviderBlock().accept(this),
				classGenricList);
	}

	@Override
	public ObjectProviderBlock visitObjectProviderBlock(ConcurnasParser.ObjectProviderBlockContext ctx) {
		ObjectProviderBlock ret = new ObjectProviderBlock(ctx.start.getLine(), ctx.start.getCharPositionInLine());
		ctx.linex.forEach(a -> ret.addLine((ObjectProviderLine)a.accept(this)));
		return ret;
	}

	@Override
	public ObjectProviderLine visitObjectProviderLine(ConcurnasParser.ObjectProviderLineContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		
		
		if(ctx.provide != null) {
			ObjectProviderLineProvide ret;
			
			Type tt = (Type)ctx.provide.accept(this);
			if(ctx.lazy != null) {
				tt = Utils.convertToLazyType((Type)tt);
			}
			
			if(ctx.provName != null) {
				ret = new ObjectProviderLineProvide(line, col, tt, ctx.provName.getText()); 
			}else {
				ret = new ObjectProviderLineProvide(line, col, tt);
			}
			
			if(ctx.fieldName != null) {
				ret.fieldName=fieldNameString((Expression)ctx.fieldName.accept(this));
			}
			
			if(ctx.provideExpr != null) {
				ret.provideExpr = new Block(line, col);
				ret.provideExpr.isolated=true;
				ret.provideExpr.setShouldBePresevedOnStack(true);
				
				ret.provideExpr.add(new LineHolder(new DuffAssign((Expression)ctx.provideExpr.accept(this))));
			}else if(ctx.objectProviderNestedDeps() != null) {
				ArrayList<ObjectProviderLineDepToExpr> nestedDeps = (ArrayList<ObjectProviderLineDepToExpr>)ctx.objectProviderNestedDeps().accept(this);
				ret.nestedDeps = nestedDeps;
			}
			
			ArrayList<Pair<String, NamedType>> localGens = null != ctx.genericQualiList() ? (ArrayList<Pair<String, NamedType>>) ctx.genericQualiList().accept(this) : new ArrayList<Pair<String, NamedType>>();
			ret.setLocalGens(localGens);
			
			if(null != ctx.pppNoInject()) {
				ret.accessModi= (AccessModifier)ctx.pppNoInject().accept(this);
			}
			
			if(ctx.single != null) {
				ret.single = true;
			}
			
			if(ctx.shared != null) {
				ret.shared = true;
			}
			
			return ret;
			
		}else {
			return (ObjectProviderLineDepToExpr)ctx.opdl.accept(this);
		}
	}
	
	@Override
	public ArrayList<ObjectProviderLineDepToExpr> visitObjectProviderNestedDeps(ConcurnasParser.ObjectProviderNestedDepsContext ctx) {
		ArrayList<ObjectProviderLineDepToExpr> nestedDeps = new ArrayList<ObjectProviderLineDepToExpr>(ctx.nestedDep.size());
		for(ConcurnasParser.ObjectProviderLineDepContext dc : ctx.nestedDep) {
			nestedDeps.add((ObjectProviderLineDepToExpr)dc.accept(this));
		}
		return nestedDeps;
	}
	
	
	private String fieldNameString(Expression what) {
		if(what instanceof VarString) {
			return ((VarString)what).str;
		}else {//what instanceof VarChar
			return ((VarChar)what).chr;
		}
		
	}
	
	@Override
	public ObjectProviderLineDepToExpr visitObjectProviderLineDep(ConcurnasParser.ObjectProviderLineDepContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();
		
		Type tt = (Type)ctx.nameFrom.accept(this);
		if(ctx.lazy != null) {
			tt = Utils.convertToLazyType((Type)tt);
		}
		
		
		
		ObjectProviderLineDepToExpr ret = new ObjectProviderLineDepToExpr(line, col, tt, ctx.exprTo != null ? (Expression)ctx.exprTo.accept(this):null, ctx.single != null, ctx.shared != null, ctx.fieldName != null?fieldNameString((Expression)ctx.fieldName.accept(this)):null, ctx.typeOnlyRHS != null?(Type)ctx.typeOnlyRHS.accept(this):null);
		
		if(ctx.objectProviderNestedDeps() != null) {
			ArrayList<ObjectProviderLineDepToExpr> nestedDeps = (ArrayList<ObjectProviderLineDepToExpr>)ctx.objectProviderNestedDeps().accept(this);
			ret.nestedDeps=nestedDeps;
		}
		
		return ret; 
	}
	
	
	@Override
	public ClassDefArgs visitObjectProviderArgs(ConcurnasParser.ObjectProviderArgsContext ctx) {
		ArrayList<ClassDefArg> args = new ArrayList<ClassDefArg>();

		ctx.objectProviderArg().forEach(a -> args.add((ClassDefArg) a.accept(this)));

		return new ClassDefArgs(ctx.start.getLine(), ctx.start.getCharPositionInLine(), args);
	}
	
	@Override
	public ClassDefArg visitObjectProviderArg(ConcurnasParser.ObjectProviderArgContext ctx) {
		int line = ctx.start.getLine();
		int col = ctx.start.getCharPositionInLine();

		AccessModifier accessModi = (AccessModifier)(ctx.pppNoInject() != null ? ctx.pppNoInject().accept(this) : null);

		boolean isFinal = ctx.isFinal != null;

		Type type = ctx.type() != null ? (Type) ctx.type().accept(this) : null;

		int refCnt = ctx.refCnt.size();
		if (refCnt > 0) {
			for (int n = 0; n < refCnt; n++) {
				type = new NamedType(line, col, type == null ? NamedType.ntObj : type);
			}
		}
		
		String name = ctx.NAME()==null?null:ctx.NAME().getText();

		ClassDefArg ret = new ClassDefArg(line, col, accessModi, isFinal, null, name, type);

		ret.annotations = ctx.annotations() == null ? null : (Annotations) ctx.annotations().accept(this);
		ret.isVararg = ctx.isvararg != null;
		if (ctx.expr_stmt() != null) {
			ret.defaultValue = (Expression) ctx.expr_stmt().accept(this);
		}
		
		if(null != ctx.transAndShared) {
			Thruple<Boolean, Boolean, Boolean> transAndShared = (Thruple<Boolean, Boolean, Boolean>)ctx.transAndShared.accept(this);
			ret.isTransient = transAndShared.getA();
			ret.isShared = transAndShared.getB();
			ret.isLazy = transAndShared.getC();
		}else {
			ret.isTransient = false;
			ret.isShared = false;
			ret.isLazy = false;
		}

		return ret;
	}
	
}
