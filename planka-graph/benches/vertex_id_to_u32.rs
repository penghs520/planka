use criterion::{criterion_group, criterion_main, Criterion};
use dashmap::DashMap;
use uuid::Uuid;
use pgraph::utils::generate_uuid_v1;

fn vertex_id_to_u32_map_get(c: &mut Criterion) {
    //26.064 ns 28.160 ns 30.977 ns
    //macbook pro m4 48G 16核: 9.3137 ns
    //查询性能不会跟着entry数量而变慢
    let vertex_id_to_u32_map: DashMap<Uuid, u32> = DashMap::with_capacity(5000_0000);
    let uuid = generate_uuid_v1();
    for i in 0..5000_0000u32 {
        if i == 1000 {
            vertex_id_to_u32_map.insert(uuid, i);
        } else {
            vertex_id_to_u32_map.insert(generate_uuid_v1(), i);
        }
    }
    c.bench_function("vertex_id_to_u32_map_get", |b| {
        b.iter(|| {
            vertex_id_to_u32_map.get(&uuid);
        });
    });
}

criterion_group!(bincode_bench, vertex_id_to_u32_map_get);
criterion_main!(bincode_bench); // cargo bench --bench xxx

