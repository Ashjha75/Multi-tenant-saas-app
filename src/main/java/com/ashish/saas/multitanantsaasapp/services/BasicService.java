package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;

public interface BasicService<I ,O>{
    void create(final I request);
    void update(final String id,final I request);
    O findByID(final String id);
    PageResponse<O> findAll(final int page, final int size);
    void delete(final String id);
}
