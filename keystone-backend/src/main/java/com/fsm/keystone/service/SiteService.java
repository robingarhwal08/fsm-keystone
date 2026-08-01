package com.fsm.keystone.service;

import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public Site createSite(Site site) {

        Customer customer =
                customerRepository
                        .findById(
                                site.getCustomer().getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found with id: "
                                                + site.getCustomer().getId()));

        site.setCustomer(customer);

        return siteRepository.save(site);
    }

    public List<Site> getAllSites() {

        return siteRepository.findAll();
    }

    public List<Site> getSitesByCustomerId(
            Long customerId) {

        return siteRepository.findByCustomerId(
                customerId);
    }

    public Site updateSite(
            Long id,
            Site input) {

        Site site =
                siteRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found with id: " + id));

        Customer customer =
                customerRepository.findById(
                                input.getCustomer().getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"));

        site.setSiteName(
                input.getSiteName());

        site.setAddress(
                input.getAddress());

        site.setCity(
                input.getCity());

        site.setState(
                input.getState());

        site.setPincode(
                input.getPincode());

        site.setContactPerson(
                input.getContactPerson());

        site.setContactPhone(
                input.getContactPhone());

        site.setCustomer(customer);

        return siteRepository.save(site);
    }

    public void deleteSite(Long id) {

        Site site =
                siteRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Site not found with id: " + id));

        siteRepository.delete(site);
    }
}