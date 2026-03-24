select
    status as status,
    cast(count(*) as bigint) as order_count,
    cast(sum(total) as bigint) as total_sum
from purchase_orders
where (:status is null or status = :status)
group by status
order by status
