package com.fsm.keystone.service;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.entity.Customer;
import com.fsm.keystone.entity.Site;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.exception.BusinessException;
import com.fsm.keystone.repository.CustomerRepository;
import com.fsm.keystone.repository.SiteRepository;
import com.fsm.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final CascadeDeleteService cascadeDeleteService;

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
        AppUser actor = resolveActor();
        if (actor != null && actor.getRole() == Role.CUSTOMER) {
            Customer customer = actor.getCustomer();
            if (customer == null) {
                return List.of();
            }
            return siteRepository.findByCustomerId(customer.getId());
        }
        return siteRepository.findAll();
    }

    public List<Site> getSitesByCustomerId(Long customerId) {
        AppUser actor = resolveActor();
        if (actor != null && actor.getRole() == Role.CUSTOMER) {
            Customer customer = actor.getCustomer();
            if (customer == null || !customer.getId().equals(customerId)) {
                throw new BusinessException("You can only view sites for your organisation");
            }
        }
        return siteRepository.findByCustomerId(customerId);
    }

    private AppUser resolveActor() {
        try {
            AppUser actor = currentUserService.requireUser();
            return userRepository.findByEmailWithCustomer(actor.getEmail()).orElse(actor);
        } catch (Exception ex) {
            return null;
        }
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
        siteRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Site not found with id: " + id));
        cascadeDeleteService.deleteSite(id);
    }
}