package com.xcz.afcs.mybatis.provider;


import com.xcz.afcs.core.model.Pagination;
import com.xcz.afcs.mybatis.model.*;
import com.xcz.afcs.mybatis.provider.params.BaseParam;
import com.xcz.afcs.mybatis.util.EntityUtil;
import com.xcz.afcs.mybatis.util.EntityViewUtil;
import org.apache.ibatis.jdbc.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mac on 2017/8/11.
 */
public class BaseSQLProvider  {

     private static final Logger logger = LoggerFactory.getLogger(BaseSQLProvider.class);

     public <T> String findAllSQL(final Class<T> entityCls) {
         final EntityModel model = EntityUtil.parseEntity(entityCls);
         String sql = new SQL() {
             {
                 SELECT(getColums(model).toArray(new String[0]));
                 FROM(model.getTableName());
                 ORDER_BY(model.getPrimaryField().getCloumnName()+" DESC ");
             }
         }.toString();
         sql += " LIMIT 0, 500";
         return sql;
     }

    public <T> String countSQL(EntityCriteria<T> criteria) {
        final EntityModel model   = EntityUtil.parseEntity(criteria.getEntityClass());
        final List<String> wheres = getWhereSQL(criteria.getExpressionList());
        String sql = new SQL() {
            {
                SELECT("count(1)");
                FROM(model.getTableName());
                WHERE(wheres.toArray(new String[0]));
            }
        }.toString();
        return sql;
    }

    public final <T> String querySQL(EntityCriteria<T> criteria) {
        final EntityModel model   = EntityUtil.parseEntity(criteria.getEntityClass());
        final List<String> wheres = getWhereSQL(criteria.getExpressionList());
        final List<String> orders = getOrderSQL(criteria.getOrderList(), model.getPrimaryField());
        final Pagination page     = criteria.getPagination();
        String sql = new SQL() {
            {
                SELECT(getSelectColumns(criteria, model).toArray(new String[0]));
                FROM(model.getTableName());
                WHERE(wheres.toArray(new String[0]));
                ORDER_BY(orders.toArray(new String[0]));
            }
        }.toString();
        if (page != null) {
            sql += " LIMIT "+page.getOffset()+","+page.getPageSize();
        }
        return sql;
    }

    /*
    public <T> String queryViewSQL(final EntityCriteria<T> criteria) {
        final EntityModel model   = EntityUtil.parseEntity(criteria.getEntityClass());
        final List<String> wheres = getWhereSQL(criteria.getExpressionList());
        final List<String> orders = getOrderSQL(criteria.getOrderList(), model.getPrimaryField());
        final Pagination page     = criteria.getPagination();
        String sql = new SQL() {
            {
                SELECT(getSelectColumns(criteria, model).toArray(new String[0]));
                FROM(model.getTableName());
                WHERE(wheres.toArray(new String[0]));
                ORDER_BY(orders.toArray(new String[0]));
            }
        }.toString();
        if (page != null) {
            sql += " LIMIT "+page.getOffset()+","+page.getPageSize();
        }
        return sql;
    }*/

    public <T> String insertSQL(final T entity) {
         final EntityModel model = EntityUtil.parseEntity(entity.getClass());
         return new SQL() {
             {
                 INSERT_INTO(model.getTableName());
                 INTO_COLUMNS(getColums(model).toArray(new String[0]));
                 INTO_VALUES(getValues(model).toArray(new String[0]));
             }
         }.toString();
     }

    public <T> String batchInsertSQL(Map<String, Object> params) {
        final List<T> entityList = (List<T>)params.get("entityList");
        final EntityModel model = EntityUtil.parseEntity(entityList.get(0).getClass());
        MessageFormat format    = new MessageFormat(getBatchValuesFormat(model));
        String sql = new SQL() {
            {
                INSERT_INTO(model.getTableName());
                INTO_COLUMNS(getColums(model).toArray(new String[0]));

            }
        }.toString();
        StringBuilder sb = new StringBuilder(sql);
        sb.append(" VALUES ");
        for(int i=0; i<entityList.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(format.format(new Object[]{"entityList["+i+"]"}));
        }
        return sb.toString();
    }

    public <T> String getOneSQL(final Map<String, Object> params ) {
        final Class<T> entityCls = (Class<T>) params.get("entityClass");
        final EntityModel model = EntityUtil.parseEntity(entityCls);
        return new SQL() {
            {
                SELECT(getColums(model).toArray(new String[0]));
                FROM(model.getTableName());
                WHERE(model.getPrimaryField().getCloumnName()+" = #{id}");
            }
        }.toString();
    }

    public <T> String updateSQL(final T entity) {
        final EntityModel model = EntityUtil.parseEntity(entity.getClass());
        return new SQL() {
            {
                UPDATE(model.getTableName());
                SET(getUpdateSets(model, false).toArray(new String[0]));
                WHERE(model.getPrimaryField().getCloumnName()+" = #{"+model.getPrimaryField().getFieldName()+"}");
            }
        }.toString();
    }

    public <T> String updateCasSQL(final T entity) {
        final EntityModel model = EntityUtil.parseEntity(entity.getClass());
        return new SQL() {
            {
                UPDATE(model.getTableName());
                SET(getUpdateSets(model, true).toArray(new String[0]));
                WHERE(model.getPrimaryField().getCloumnName()+" = #{"+model.getPrimaryField().getFieldName()+"}");
                WHERE("version = #{version}");
            }
        }.toString();
    }


    public <T> String deleteSQL(final Map<String, Object> params) {
        final Class<T> entityCls = (Class<T>) params.get("entityClass");
        final EntityModel model = EntityUtil.parseEntity(entityCls);
        return new SQL() {
            {
                DELETE_FROM(model.getTableName());
                WHERE(model.getPrimaryField().getCloumnName()+" = #{id}");
            }
        }.toString();
    }

    private <T> List<String> getOrderSQL(List<Order> orderList, EntityField primary) {
        List<String> orders = new ArrayList<String>();
        if (orderList.size() == 0) {
            orders.add(primary.getCloumnName()+" DESC ");
            return orders;
        }
        for (Order order : orderList) {
            orders.add(order.getCloumnName() + (order.isAscending() ? " ASC " : "DESC"));
        }
        return orders;
    }

    private <T> List<String> getWhereSQL(List<Expression> expressionList) {
        List<String> wheres = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (Expression expression : expressionList) {
            sb.append(expression.getCloumnName());
            switch (expression.getExp()) {
                case EQ:
                     sb.append(" = ");
                     sb.append("#{params.").append(expression.getParamName()).append("}");
                     break;
                case LIKE:
                    sb.append(" LIKE ");
                    sb.append("CONCAT").append("('%',");
                    sb.append("#{params.").append(expression.getParamName()).append("}");
                    sb.append(",'%')");
                    break;
                case GT:
                    sb.append(" > ");
                    sb.append("#{params.").append(expression.getParamName()).append("}");
                    break;
                case LT:
                    sb.append(" < ");
                    sb.append("#{params.").append(expression.getParamName()).append("}");
                    break;
                case GTE:
                    sb.append(" >= ");
                    sb.append("#{params.").append(expression.getParamName()).append("}");
                    break;
                case LTE:
                    sb.append(" <= ");
                    sb.append("#{params.").append(expression.getParamName()).append("}");
                    break;
                case IN:
                    sb.append(" IN (");
                    List<?> list = (List<?>)expression.getValue();
                    for(int i=0; i<list.size(); i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append("#{params.").append(expression.getParamName()).append("[").append(i).append("]").append("}");
                    }
                    sb.append(")");
                    break;

            }
            wheres.add(sb.toString());
            sb.delete(0, sb.length()); //清空
        }
        return wheres;
    }

    private List<String> getColums(EntityModel model) {
        List<String> columns = new ArrayList();
        columns.add(model.getPrimaryField().getCloumnName());
        for (EntityField entityField: model.getFieldList()) {
            if (entityField.getCloumnName().equals(entityField.getFieldName())) {
                columns.add(entityField.getCloumnName());
            }else {
                columns.add(entityField.getCloumnName()+" "+entityField.getFieldName());
            }
        }
        return columns;
    }

    public <T> List<String> getSelectColumns(EntityCriteria<T> criteria, EntityModel model) {
         List<String> columns = new ArrayList();
         if (criteria.getSelectColumns().size() != 0) {
             return criteria.getSelectColumns();
         }
         if (criteria.getResultViewCls() != null) {
             List<String> fieldsNames = EntityViewUtil.parseEntityViewField(criteria.getResultViewCls());
             if (fieldsNames.size() == 0) {
                 throw new RuntimeException("返回的对象必须包含一个属性字段");
             }
             for (String filedName : fieldsNames) {
                 EntityField ef = model.getEntityFieldByName(filedName);
                 if (ef == null) {
                     continue;
                 }
                 if (filedName.equals(ef.getCloumnName())) {
                     columns.add(ef.getCloumnName());
                 }else{
                     columns.add(ef.getCloumnName()+" "+filedName);
                 }
             }
             return columns;
         }else {
             return getColums(model);
         }
    }


    private List<String> getValues(EntityModel model) {
        List<String> values = new ArrayList();
        values.add("#{"+model.getPrimaryField().getCloumnName()+"}");
        for (EntityField entityField: model.getFieldList()) {
            values.add("#{"+entityField.getFieldName()+"}");
        }
        return values;
    }


    private List<String> getUpdateSets(EntityModel model, boolean cas) {
        List<String> sets = new ArrayList();
        for (EntityField entityField: model.getFieldList()) {
            if (entityField.getFieldName().equals(model.getPrimaryField().getFieldName())) {
                continue;
            }
            if (cas) {
                if ("version".equals(entityField.getFieldName())) {
                    sets.add(entityField.getCloumnName()+"= #{"+entityField.getFieldName()+"}+1");
                }else {
                    sets.add(entityField.getCloumnName()+"= #{"+entityField.getFieldName()+"}");
                }
            }else{
                sets.add(entityField.getCloumnName()+"= #{"+entityField.getFieldName()+"}");
            }
        }
        return sets;
    }

    private String getBatchValuesFormat(EntityModel model) {
         StringBuilder sb = new StringBuilder();
         sb.append("(");
         sb.append("#'{'{0}."+model.getPrimaryField().getCloumnName()+"}");
         for (EntityField entityField: model.getFieldList()) {
             sb.append(", #'{'{0}."+entityField.getFieldName()+"}");
         }
         sb.append(")");
         return sb.toString();
     }

     public String getOrderAndPageSQL(BaseParam param) {
         SQL sql = new SQL();
         if (param.getOrderList() != null) {
             for (Order order : param.getOrderList()) {
                  if (order.isAscending()) {
                      sql.ORDER_BY(order.getPropertyName()+" ASC ");
                  }else {
                      sql.ORDER_BY(order.getPropertyName()+" DESC ");
                  }
             }
         }
         if (param.getPage() != null) {
             return sql.toString()+" LIMIT #{page.offset}, #{page.pageSize}";
         }
         return sql.toString();
     }


}
