package com.ashish.saas.multitanantsaasapp.services;

import java.util.List;

public interface BasicService<I ,O>{
    void create(final I request);
    void update(final String id,final I request);
    O findByID(final String id);
    List<O> findAll();
    void delete(final String id);
}
