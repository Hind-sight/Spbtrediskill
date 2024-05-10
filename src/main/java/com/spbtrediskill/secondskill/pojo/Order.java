package com.spbtrediskill.secondskill.pojo;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
@Data
@Table(name = "t_order")
public class Order implements Serializable {
    private static final long serialVersionUID = -8867272732777764701L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_name")
    private String order_name;

    @Column(name = "order_user")
    private String order_user;
}


