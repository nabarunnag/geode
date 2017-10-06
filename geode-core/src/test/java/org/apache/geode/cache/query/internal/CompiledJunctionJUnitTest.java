/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.cache.query.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.query.internal.parse.OQLLexerTokenTypes;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class CompiledJunctionJUnitTest {

  @Test
  public void oneIteratorHasOperandsWithIndexesAndOneOperandWithoutAnIndexShouldBeSelected() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandsForB =
        new CompiledJunction.OperandAndIndexCount();

    operandsForA.addToIndexCount(1);
    operandsForA.addToOperandCount(1);

    operandsForB.addToIndexCount(1);
    operandsForB.addToOperandCount(2);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandsForB);

    assertEquals(iteratorB,
        junction.getIteratorWhoseOperandsWeWantToKeep(iteratorToOperandAndIndexCounts));
  }

  @Test
  public void oneIteratorHasOperandsWithIndexesAndOneOperandWithoutAnIndexShouldBeSelectedWhenMoreThanTwoIteratorsCanBeSelected() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandsForA.addToIndexCount(1);
    operandsForA.addToOperandCount(1);

    operandsForB.addToIndexCount(1);
    operandsForB.addToOperandCount(2);

    operandsForC.addToIndexCount(1);
    operandsForC.addToOperandCount(1);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandsForC);

    assertEquals(iteratorB,
        junction.getIteratorWhoseOperandsWeWantToKeep(iteratorToOperandAndIndexCounts));
  }

  @Test
  public void iteratorWhichHasMoreOperandsWithIndexesShouldBeSelectedWhenMultipleSuchIteratorsCanBeSelected() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandsForA.addToIndexCount(5);
    operandsForA.addToOperandCount(5);

    operandsForB.addToIndexCount(3);
    operandsForB.addToOperandCount(3);

    operandsForC.addToIndexCount(4);
    operandsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandsForC);

    assertEquals(iteratorA,
        junction.getIteratorWhoseOperandsWeWantToKeep(iteratorToOperandAndIndexCounts));
  }

  @Test
  public void manipulateTreeToOptimizeForJoinShouldMoveAllOperandsNotBelongingToSelectedIteratorIntoEvalOperandsList()
      throws Exception {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    CompiledValue operand2ForA = mock(CompiledValue.class);
    CompiledValue operand3ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);
    operandsForA.add(operand2ForA);
    operandsForA.add(operand3ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    CompiledValue operand3ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);
    operandsForB.add(operand3ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    CompiledValue operand3ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);
    operandsForC.add(operand3ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    CompiledJunction.ManipulatedOperands returnedOperands =
        junction.manipulateTreeToOptimizeForJoin(evalOperands, 0, iterToOperands,
            iteratorToOperandAndIndexCounts);
    assertEquals(6, returnedOperands.evalOperands.size());
    assertTrue(returnedOperands.evalOperands.contains(operand1ForB));
    assertTrue(returnedOperands.evalOperands.contains(operand2ForB));
    assertTrue(returnedOperands.evalOperands.contains(operand3ForB));
    assertTrue(returnedOperands.evalOperands.contains(operand1ForC));
    assertTrue(returnedOperands.evalOperands.contains(operand2ForC));
    assertTrue(returnedOperands.evalOperands.contains(operand3ForC));
  }

  @Test
  public void manipulatedTreeShouldNotLoseOriginalOperandsInEvalOperands() throws Exception {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    CompiledJunction.ManipulatedOperands returnedOperands =
        junction.manipulateTreeToOptimizeForJoin(evalOperands, 0, iterToOperands,
            iteratorToOperandAndIndexCounts);
    assertEquals(5, returnedOperands.evalOperands.size());
    assertTrue(returnedOperands.evalOperands.contains(originalOperand));
    assertTrue(returnedOperands.evalOperands.contains(operand1ForB));
    assertTrue(returnedOperands.evalOperands.contains(operand2ForB));
    assertTrue(returnedOperands.evalOperands.contains(operand1ForC));
    assertTrue(returnedOperands.evalOperands.contains(operand2ForC));
  }


  @Test
  public void joinManipulationShouldNotBeAllowedIfCompositeFilterOpsMapSizeIsNotGreaterThanZero() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    Map compositeFilterOpsMap = new LinkedHashMap();
    assertFalse(junction.isJoinManipulationAllowed(compositeFilterOpsMap, iterToOperands,
        iteratorToOperandAndIndexCounts));

  }

  @Test
  public void joinManipulationShouldNotBeAllowedIfIteratorsSizeIsNotGreaterThanOne() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);

    Map compositeFilterOpsMap = new LinkedHashMap();
    compositeFilterOpsMap.put("dummy_key_1", "dummy_value_1");
    compositeFilterOpsMap.put("dummy_key_2", "dummy_value_2");
    compositeFilterOpsMap.put("dummy_key_3", "dummy_value_3");
    assertFalse(junction.isJoinManipulationAllowed(compositeFilterOpsMap, iterToOperands,
        iteratorToOperandAndIndexCounts));

  }

  @Test
  public void joinManipulationShouldNotBeAllowedIfIteratorsAreNotSquashable() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(6);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(4);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(5);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    Map compositeFilterOpsMap = new LinkedHashMap();
    compositeFilterOpsMap.put("dummy_key_1", "dummy_value_1");
    compositeFilterOpsMap.put("dummy_key_2", "dummy_value_2");
    compositeFilterOpsMap.put("dummy_key_3", "dummy_value_3");
    assertFalse(junction.isJoinManipulationAllowed(compositeFilterOpsMap, iterToOperands,
        iteratorToOperandAndIndexCounts));

  }


  @Test
  public void joinManipulationShouldNotBeAllowedIfThereisAnIteratorWithNoIndexes() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(0);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    Map compositeFilterOpsMap = new LinkedHashMap();
    compositeFilterOpsMap.put("dummy_key_1", "dummy_value_1");
    compositeFilterOpsMap.put("dummy_key_2", "dummy_value_2");
    compositeFilterOpsMap.put("dummy_key_3", "dummy_value_3");
    assertFalse(junction.isJoinManipulationAllowed(compositeFilterOpsMap, iterToOperands,
        iteratorToOperandAndIndexCounts));

  }

  @Test
  public void joinManipulationShouldBeAllowedIfAllRequiredConditionsAreMet() {
    CompiledValue[] compiledValues = new CompiledValue[] {null, null};
    CompiledJunction junction =
        new CompiledJunction(compiledValues, OQLLexerTokenTypes.LITERAL_and);

    Map<RuntimeIterator, CompiledJunction.OperandAndIndexCount> iteratorToOperandAndIndexCounts =
        new HashMap();
    CompiledJunction.OperandAndIndexCount operandCountsForA =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForB =
        new CompiledJunction.OperandAndIndexCount();
    CompiledJunction.OperandAndIndexCount operandCountsForC =
        new CompiledJunction.OperandAndIndexCount();

    operandCountsForA.addToIndexCount(5);
    operandCountsForA.addToOperandCount(5);

    operandCountsForB.addToIndexCount(3);
    operandCountsForB.addToOperandCount(3);

    operandCountsForC.addToIndexCount(4);
    operandCountsForC.addToOperandCount(4);

    RuntimeIterator iteratorA = mock(RuntimeIterator.class);
    RuntimeIterator iteratorB = mock(RuntimeIterator.class);
    RuntimeIterator iteratorC = mock(RuntimeIterator.class);

    iteratorToOperandAndIndexCounts.put(iteratorA, operandCountsForA);
    iteratorToOperandAndIndexCounts.put(iteratorB, operandCountsForB);
    iteratorToOperandAndIndexCounts.put(iteratorC, operandCountsForC);

    List evalOperands = new ArrayList();
    CompiledValue originalOperand = mock(CompiledValue.class);
    evalOperands.add(originalOperand);

    List<CompiledValue> operandsForA = new ArrayList<>();
    CompiledValue operand1ForA = mock(CompiledValue.class);
    operandsForA.add(operand1ForA);

    List<CompiledValue> operandsForB = new ArrayList<>();
    CompiledValue operand1ForB = mock(CompiledValue.class);
    CompiledValue operand2ForB = mock(CompiledValue.class);
    operandsForB.add(operand1ForB);
    operandsForB.add(operand2ForB);

    List<CompiledValue> operandsForC = new ArrayList<>();
    CompiledValue operand1ForC = mock(CompiledValue.class);
    CompiledValue operand2ForC = mock(CompiledValue.class);
    operandsForC.add(operand1ForC);
    operandsForC.add(operand2ForC);

    Map<RuntimeIterator, List<CompiledValue>> iterToOperands = new HashMap();
    iterToOperands.put(iteratorA, operandsForA);
    iterToOperands.put(iteratorB, operandsForB);
    iterToOperands.put(iteratorC, operandsForC);

    Map compositeFilterOpsMap = new LinkedHashMap();
    compositeFilterOpsMap.put("dummy_key_1", "dummy_value_1");
    compositeFilterOpsMap.put("dummy_key_2", "dummy_value_2");
    compositeFilterOpsMap.put("dummy_key_3", "dummy_value_3");
    assertTrue(junction.isJoinManipulationAllowed(compositeFilterOpsMap, iterToOperands,
        iteratorToOperandAndIndexCounts));

  }



}
