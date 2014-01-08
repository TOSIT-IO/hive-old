/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.vector.expressions;

import org.apache.hadoop.hive.common.type.Decimal128;
import org.apache.hadoop.hive.ql.exec.vector.expressions.VectorExpression;
import org.apache.hadoop.hive.ql.exec.vector.expressions.NullUtil;
import org.apache.hadoop.hive.ql.exec.vector.*;

/**
 * Generated from template ColumnArithmeticColumn.txt, which covers binary arithmetic
 * expressions between columns.
 */
public class DecimalColAddDecimalColumn extends VectorExpression {

  private static final long serialVersionUID = 1L;

  private int colNum1;
  private int colNum2;
  private int outputColumn;

  public DecimalColAddDecimalColumn(int colNum1, int colNum2, int outputColumn) {
    this.colNum1 = colNum1;
    this.colNum2 = colNum2;
    this.outputColumn = outputColumn;
  }

  public DecimalColAddDecimalColumn() {
  }

  @Override
  public void evaluate(VectorizedRowBatch batch) {

    if (childExpressions != null) {
      super.evaluateChildren(batch);
    }

    DecimalColumnVector inputColVector1 = (DecimalColumnVector) batch.cols[colNum1];
    DecimalColumnVector inputColVector2 = (DecimalColumnVector) batch.cols[colNum2];
    DecimalColumnVector outputColVector = (DecimalColumnVector) batch.cols[outputColumn];
    int[] sel = batch.selected;
    int n = batch.size;
    Decimal128[] vector1 = inputColVector1.vector;
    Decimal128[] vector2 = inputColVector2.vector;
    Decimal128[] outputVector = outputColVector.vector;

    // return immediately if batch is empty
    if (n == 0) {
      return;
    }

    outputColVector.isRepeating =
         inputColVector1.isRepeating && inputColVector2.isRepeating
      || inputColVector1.isRepeating && !inputColVector1.noNulls && inputColVector1.isNull[0]
      || inputColVector2.isRepeating && !inputColVector2.noNulls && inputColVector2.isNull[0];

    // Handle nulls first
    NullUtil.propagateNullsColCol(
      inputColVector1, inputColVector2, outputColVector, sel, n, batch.selectedInUse);

    /* Disregard nulls for processing. In other words,
     * the arithmetic operation is performed even if one or
     * more inputs are null. This is to improve speed by avoiding
     * conditional checks in the inner loop.
     */
    if (inputColVector1.isRepeating && inputColVector2.isRepeating) {
      addChecked(0, vector1[0], vector2[0], outputColVector);
    } else if (inputColVector1.isRepeating) {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          addChecked(i, vector1[0], vector2[i], outputColVector);
        }
      } else {
        for(int i = 0; i != n; i++) {
          addChecked(i, vector1[0], vector2[i], outputColVector);
        }
      }
    } else if (inputColVector2.isRepeating) {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          addChecked(i, vector1[i], vector2[0], outputColVector);
        }
      } else {
        for(int i = 0; i != n; i++) {
          addChecked(i, vector1[i], vector2[0], outputColVector);
        }
      }
    } else {
      if (batch.selectedInUse) {
        for(int j = 0; j != n; j++) {
          int i = sel[j];
          addChecked(i, vector1[i], vector2[i], outputColVector);
        }
      } else {
        for(int i = 0; i != n; i++) {
          addChecked(i, vector1[i], vector2[i], outputColVector);
        }
      }
    }

    /* For the case when the output can have null values, follow
     * the convention that the data values must be 1 for long and
     * NaN for double. This is to prevent possible later zero-divide errors
     * in complex arithmetic expressions like col2 / (col1 - 1)
     * in the case when some col1 entries are null.
     */
    NullUtil.setNullDataEntriesDecimal(outputColVector, batch.selectedInUse, sel, n);
  }

  // Addition with overflow check. Overflow produces NULL output.
  private static void addChecked(int i, Decimal128 left, Decimal128 right,
      DecimalColumnVector outputColVector) {
    try {
      Decimal128.add(left, right, outputColVector.vector[i], outputColVector.scale);
      outputColVector.vector[i].checkPrecisionOverflow(outputColVector.precision);
    } catch (ArithmeticException e) {  // catch on overflow
      outputColVector.noNulls = false;
      outputColVector.isNull[i] = true;
    }
  }

  @Override
  public int getOutputColumn() {
    return outputColumn;
  }

  @Override
  public String getOutputType() {
    return "long";
  }

  public int getColNum1() {
    return colNum1;
  }

  public void setColNum1(int colNum1) {
    this.colNum1 = colNum1;
  }

  public int getColNum2() {
    return colNum2;
  }

  public void setColNum2(int colNum2) {
    this.colNum2 = colNum2;
  }

  public void setOutputColumn(int outputColumn) {
    this.outputColumn = outputColumn;
  }

  @Override
  public VectorExpressionDescriptor.Descriptor getDescriptor() {
    return (new VectorExpressionDescriptor.Builder())
        .setMode(
            VectorExpressionDescriptor.Mode.PROJECTION)
        .setNumArguments(2)
        .setArgumentTypes(
            VectorExpressionDescriptor.ArgumentType.getType("decimal"),
            VectorExpressionDescriptor.ArgumentType.getType("decimal"))
        .setInputExpressionTypes(
            VectorExpressionDescriptor.InputExpressionType.COLUMN,
            VectorExpressionDescriptor.InputExpressionType.COLUMN).build();
  }
}
