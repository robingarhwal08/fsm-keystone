import WorkOrders from "./WorkOrders";

export default function CustomerRequests({ user }) {
    return (
        <WorkOrders user={user} />
    );
}