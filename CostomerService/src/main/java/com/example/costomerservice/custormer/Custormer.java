package com.example.costomerservice.custormer;

public class Custormer {
    private int custid;
    private String custname;
    private String email;
    private int phonenum;

    public Custormer(int custid, String custname, String email, int phonenum) {
        this.custid = custid;
        this.custname = custname;
        this.email = email;
        this.phonenum = phonenum;
    }

    public int getCustid() {
        return custid;
    }

    public String getCustname() {
        return custname;
    }

    public String getEmail() {
        return email;
    }

    public int getPhonenum() {
        return phonenum;
    }
}
