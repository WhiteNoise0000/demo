package com.example.poc.dao;

import com.example.poc.entity.PurchaseOrder;

/**
 * 旧 saveOrUpdate 相当の更新制御を提供する拡張Repositoryです。
 */
public interface PurchaseOrderRepositoryCustom {

    /**
     * 事前採番済みIDと事前設定済みversionを前提に、insert/updateを切り替えて保存します。
     * <p>
     * updateは `id + version` 一致時のみ成功し、未一致時は未存在判定のうえ insert 可否を判断します。
     * </p>
     *
     * @param order 保存対象
     * @return 保存後の最新状態
     */
    PurchaseOrder saveOrUpdateLikeLegacy(PurchaseOrder order);

    /**
     * 指定IDの行が永続化済みかを返します。
     *
     * @param id 主キー
     * @return 永続化済みならtrue
     */
    boolean isPersisted(Long id);
}
