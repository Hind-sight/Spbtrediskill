package com.spbtrediskill.secondskill.pojo;

import lombok.Data;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
@Table(name = "stock")
@Data
public class Stock implements Serializable {
    private static final long serialVersionUID = 2451194410162873075L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "stock")
    private Long stock;
}

