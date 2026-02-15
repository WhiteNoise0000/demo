package com.example.poc.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.poc.entity.Customer;

/**
 * 顧客マスタへのアクセスを担当するSpring Data Repositoryです。
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * 顧客名の部分一致（大文字小文字を無視）で検索します。
     *
     * @param keyword 検索キーワード
     * @return 条件に一致した顧客一覧
     */
    List<Customer> findByNameContainingIgnoreCase(String keyword);
}
