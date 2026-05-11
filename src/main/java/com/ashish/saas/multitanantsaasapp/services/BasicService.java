package com.ashish.saas.multitanantsaasapp.services;

public interface BasicService<I ,O>{
    void create(final I request);
    void update(final String id,final I request);
    O findByID(final String id);
    void delete(final String id);
}
