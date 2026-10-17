# Registros do ciclo TDD

Saídas dos testes capturadas **durante** o desenvolvimento, na ordem dos commits (`01` → `12`).
Cada par `…-RED.txt` / `…-GREEN.txt` mostra o mesmo conjunto de testes antes e depois da implementação.

- `01` a `10`: testes unitários (domínio e serviço) rodados a cada passo.
- `11`: migrações Flyway aplicadas num PostgreSQL real, conferindo índices únicos parciais, CHECK e a
  consulta da agenda do dia (equivalente SQL do JPQL).
- `12`: os 30 testes unitários ao final.

Como foram gerados: o ambiente da IA não tinha acesso ao Maven Central nem ao Docker Hub. Por isso
os unitários rodaram com um executor mínimo compatível com as anotações do JUnit e do Mockito usadas
no projeto, e o SQL rodou no PostgreSQL 18 (PGlite). **Os relatórios oficiais são os da esteira**
(artefatos `relatorio-testes-*` e o *Summary* de cada execução no GitHub Actions) e os do `mvn verify`
na máquina do grupo.
