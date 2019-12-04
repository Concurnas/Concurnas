package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.runtime.Pair;

public interface PreEntryCode {
		public void setLinesToVisitOnEntry(Stack<Pair<Label, Block>> blk); 
		public Stack<Pair<Label, Block>> getLinesToVisitOnEntry();
		public Label getLabelToVisitJustBeforeReturnOpCode() ;
		public void setLabelToVisitJustBeforeReturnOpCode(Label labelb4) ;
		public void setLabelBeforeAction(Label endLabel);
		public ArrayList<Label> extractInsertedFinSegmentsList();
}
