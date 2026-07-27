package com.example.product.model;

public class Product {
    private int prodid;
    private String prodname;
    private double price;
    private int stock;

    public Product(int prodid, String prodname , double price , int stock){
        this.prodid = prodid;
        this.prodname = prodname;
        this.price = price;
        this.stock = stock;
    }

    public int getProdid() {
        return prodid;
    }

    public void setProdid(int prodid) {
        this.prodid = prodid;
    }

    public String getProdname() {
        return prodname;
    }

    public void setProdname(String prodname) {
        this.prodname = prodname;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
