package com.chatapp.Mapper;

public interface Mapper<A, B> {

    A mapFrom(B b);

    B mapTo(A a);
}
