export function getCustomerId(user) {
  const id = user?.customerId ?? user?.customer?.id;
  return id != null ? Number(id) : null;
}

export function siteBelongsToCustomer(site, customerId) {
  if (customerId == null) {
    return false;
  }
  const siteCustomerId = site?.customer?.id ?? site?.customerId;
  return siteCustomerId != null && Number(siteCustomerId) === Number(customerId);
}

export function filterSitesForCustomer(sites, user) {
  const customerId = getCustomerId(user);
  if (!customerId) {
    return [];
  }
  return (sites || []).filter((site) => siteBelongsToCustomer(site, customerId));
}

export function workOrderBelongsToCustomer(workOrder, customerId) {
  if (customerId == null) {
    return false;
  }
  const workOrderCustomerId = workOrder?.customer?.id ?? workOrder?.customerId;
  return workOrderCustomerId != null && Number(workOrderCustomerId) === Number(customerId);
}

export function filterWorkOrdersForCustomer(workOrders, user) {
  const customerId = getCustomerId(user);
  if (!customerId) {
    return workOrders || [];
  }
  return (workOrders || []).filter((workOrder) =>
    workOrderBelongsToCustomer(workOrder, customerId)
  );
}
