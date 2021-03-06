/*
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
package org.apache.flink.api.expressions.typeinfo

import java.util

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.CompositeType.FlatFieldDescriptor
import org.apache.flink.api.common.typeutils.{CompositeType, TypeComparator, TypeSerializer}

/**
 * A TypeInformation that is used to rename fields of an underlying CompositeType. This
 * allows the system to translate "as" expression operations to a [[RenameOperator]]
 * that does not get translated to a runtime operator.
 */
class RenamingProxyTypeInfo[T](
    tpe: CompositeType[T],
    fieldNames: Array[String]) extends CompositeType[T](tpe.getTypeClass) {

  def getUnderlyingType: CompositeType[T] = tpe

  if (tpe.getArity != fieldNames.length) {
    throw new IllegalArgumentException(s"Number of field names '${fieldNames.mkString(",")}' and " +
      s"number of fields in underlying type $tpe do not match.")
  }

  if (fieldNames.toSet.size != fieldNames.length) {
    throw new IllegalArgumentException(s"New field names must be unique. " +
      s"Names: ${fieldNames.mkString(",")}.")
  }

  override def getFieldIndex(fieldName: String): Int = {
    val result = fieldNames.indexOf(fieldName)
    if (result != fieldNames.lastIndexOf(fieldName)) {
      -2
    } else {
      result
    }
  }
  override def getFieldNames: Array[String] = fieldNames

  override def isBasicType: Boolean = tpe.isBasicType

  override def createSerializer(executionConfig: ExecutionConfig): TypeSerializer[T] =
    tpe.createSerializer(executionConfig)

  override def getArity: Int = tpe.getArity

  override def isKeyType: Boolean = tpe.isKeyType

  override def getTypeClass: Class[T] = tpe.getTypeClass

  override def getGenericParameters: java.util.List[TypeInformation[_]] = tpe.getGenericParameters

  override def isTupleType: Boolean = tpe.isTupleType

  override def toString = {
    s"RenamingType(type: ${tpe.getTypeClass.getSimpleName}; " +
      s"fields: ${fieldNames.mkString(", ")})"
  }

  override def getTypeAt[X](pos: Int): TypeInformation[X] = tpe.getTypeAt(pos)

  override def getTotalFields: Int = tpe.getTotalFields

  override def createComparator(
        logicalKeyFields: Array[Int],
        orders: Array[Boolean],
        logicalFieldOffset: Int,
        executionConfig: ExecutionConfig) =
    tpe.createComparator(logicalKeyFields, orders, logicalFieldOffset, executionConfig)

  // These are never called since we override create comparator
  override protected def initializeNewComparator(localKeyCount: Int): Unit =
    throw new RuntimeException("Cannot happen.")

  override protected def getNewComparator(executionConfig: ExecutionConfig): TypeComparator[T] =
    throw new RuntimeException("Cannot happen.")

  override protected def addCompareField(fieldId: Int, comparator: TypeComparator[_]): Unit =
    throw new RuntimeException("Cannot happen.")

  override def getFlatFields(
      fieldExpression: String,
      offset: Int,
      result: util.List[FlatFieldDescriptor]): Unit = {
    tpe.getFlatFields(fieldExpression, offset, result)
  }

  override def getTypeAt[X](fieldExpression: String): TypeInformation[X] = {
    tpe.getTypeAt(fieldExpression)
  }
}
