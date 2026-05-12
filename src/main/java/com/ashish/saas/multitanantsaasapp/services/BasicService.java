package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BasicService<I ,O>{
    void create(final I request);
    void update(final String id,final I request);
    O findByID(final String id);
    PageResponse<O> findAll();
    void delete(final String id);
}
