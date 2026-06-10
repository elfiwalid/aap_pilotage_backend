\set output_file 'ml/datasets/resource_forecasting_dataset.csv'

\echo Generating resource forecasting dataset into :output_file

\copy (
with prevision_months as (
    select distinct
        pr.projet_id,
        extract(year from gs)::int as annee,
        extract(month from gs)::int as mois
    from previsions pr
    cross join lateral generate_series(
        date_trunc('month', pr.periode_debut)::date,
        date_trunc('month', pr.periode_fin)::date,
        interval '1 month'
    ) gs
    where pr.projet_id is not null
      and pr.periode_debut is not null
      and pr.periode_fin is not null
),
affectation_months as (
    select distinct
        a.projet_id,
        extract(year from gs)::int as annee,
        extract(month from gs)::int as mois
    from affectations a
    cross join lateral generate_series(
        date_trunc('month', a.date_debut)::date,
        date_trunc('month', a.date_fin)::date,
        interval '1 month'
    ) gs
    where a.projet_id is not null
      and a.date_debut is not null
      and a.date_fin is not null
),
project_months as (
    select * from prevision_months
    union
    select * from affectation_months
),
month_bounds as (
    select
        pm.projet_id,
        pm.annee,
        pm.mois,
        make_date(pm.annee, pm.mois, 1)::date as month_start,
        (make_date(pm.annee, pm.mois, 1) + interval '1 month - 1 day')::date as month_end
    from project_months pm
),
affectation_features as (
    select
        mb.projet_id,
        mb.annee,
        mb.mois,
        count(distinct a.collaborateur_id) as nb_collaborateurs_actuels,
        coalesce(avg(coalesce(a.taux_affectation, a.charge_prevue, 0.0)), 0.0) as charge_moyenne,
        coalesce(max(coalesce(a.taux_affectation, a.charge_prevue, 0.0)), 0.0) as charge_max
    from month_bounds mb
    left join affectations a
      on a.projet_id = mb.projet_id
     and a.date_debut <= mb.month_end
     and a.date_fin >= mb.month_start
    group by mb.projet_id, mb.annee, mb.mois
),
anomaly_features as (
    select
        mb.projet_id,
        mb.annee,
        mb.mois,
        count(av2.id) filter (where av2.type_anomalie = 'CONFLIT') as nb_conflits,
        count(av2.id) filter (where av2.type_anomalie = 'SURCHARGE') as nb_surcharges,
        count(av2.id) filter (where av2.type_anomalie = 'SOUS_CHARGE') as nb_sous_charges,
        count(av2.id) as nb_anomalies_total,
        count(distinct av2.collaborateur_id) as nb_collaborateurs_concernes
    from month_bounds mb
    join projets p on p.id = mb.projet_id
    left join anomalies_v2 av2
      on av2.annee = mb.annee
     and av2.mois = mb.mois
     and av2.projets_concernes is not null
     and lower(av2.projets_concernes) like '%' || lower(p.nom) || '%'
    group by mb.projet_id, mb.annee, mb.mois
),
target_features as (
    select
        mb.projet_id,
        mb.annee,
        mb.mois,
        count(distinct a.collaborateur_id) as target_besoin_ressources_mois_suivant
    from month_bounds mb
    left join affectations a
      on a.projet_id = mb.projet_id
     and a.date_debut <= (mb.month_end + interval '1 month')::date
     and a.date_fin >= (mb.month_start + interval '1 month')::date
    group by mb.projet_id, mb.annee, mb.mois
)
select
    mb.projet_id,
    mb.mois,
    mb.annee,
    coalesce((p.date_fin - p.date_debut), 0) as duree_projet_jours,
    coalesce(af.nb_collaborateurs_actuels, 0) as nb_collaborateurs_actuels,
    round(coalesce(af.charge_moyenne, 0.0)::numeric, 2) as charge_moyenne,
    round(coalesce(af.charge_max, 0.0)::numeric, 2) as charge_max,
    coalesce(an.nb_conflits, 0) as nb_conflits,
    coalesce(an.nb_surcharges, 0) as nb_surcharges,
    coalesce(an.nb_sous_charges, 0) as nb_sous_charges,
    coalesce(an.nb_anomalies_total, 0) as nb_anomalies_total,
    coalesce(an.nb_collaborateurs_concernes, 0) as nb_collaborateurs_concernes,
    coalesce(tf.target_besoin_ressources_mois_suivant, 0) as target_besoin_ressources_mois_suivant
from month_bounds mb
join projets p on p.id = mb.projet_id
left join affectation_features af
  on af.projet_id = mb.projet_id and af.annee = mb.annee and af.mois = mb.mois
left join anomaly_features an
  on an.projet_id = mb.projet_id and an.annee = mb.annee and an.mois = mb.mois
left join target_features tf
  on tf.projet_id = mb.projet_id and tf.annee = mb.annee and tf.mois = mb.mois
order by mb.projet_id, mb.annee, mb.mois
) to :'output_file' with (format csv, header true, encoding 'UTF8');

\echo Done.
