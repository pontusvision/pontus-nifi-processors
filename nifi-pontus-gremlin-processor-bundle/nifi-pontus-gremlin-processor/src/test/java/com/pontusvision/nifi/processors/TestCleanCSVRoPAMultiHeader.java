/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pontusvision.nifi.processors;


import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author phillip
 */
public class TestCleanCSVRoPAMultiHeader {
  final static String ropaHeader = "1.1 Identificação do Processo,,,,,1.2 Responsável pelo Processo de Negócio,,,,,,1.3 Informações do Controlador,,,1.4 Informações do Operador,,,1.5 Informe todos os produtos/serviços relacionados a este processo de negócio. (Vide guide),2.1 Nível de criticidade do processo de negócio. (Vide guide),2.2 O preenchimento do DPIA é necessário para este processo de negócio? (vide guide),2.3 Avaliação de Impacto de Privacidade de Dados (DPIA) foi realizada?,2.4 O relatório do DPIA foi aprovado pelo Jurídico?,2.5 O relatório do DPIA possui planos de ação recomendados pelo Jurídico? Em caso positivo\\, detalhar,2.6 O relatório do DPIA foi aprovado por SI?,2.7 O relatório do DPIA possui planos de ação recomendados por SI? Em caso positivo\\, detalhar,3.1 O \"Legítimo Interesse\" foi utilizado como base legal para o tratamento de dados pessoais?,3.2 O preenchimento da Avaliação de Legítimo  Interesse (formulário LIA) foi realizado?,3.3 O relatório do LIA foi validado pelo Jurídico?,4.1 Que tipo de dado pessoal é tratado?,,,,,,,5.1 Coleta de Dados Pessoais (Vide Guide),,,,,,,,,,,,,,,,,,,,,,,,,,,5.2 Transferência de dados pessoais,,,,,,,,,,,,,,,,,,,,,6.1 Os dados pessoais são armazenados após a conclusão do tratamento dos dados\\, ou seja\\, após atingida a finalidade pretendida\\, os dados ainda são mantidos pela Telefonica?,,,,,,,6.2 Os dados pessoais são excluídos após o período de retenção ou após a conclusão do tratamento dos dados?,,7.1 Privacy by Design,,7.2 Questionamentos/Sanções da ANPD,,,,,,7.3 Incidentes de Segurança,,,,7.4 Consentimento\n" +
      "1.1.1 ID do Processo,1.1.2 Área de Negócio responsável pelo Processo,1.1.3 Nome do Processo,1.1.4 Descrição e Finalidade do Processo de Negócio,1.1.5 Nome do Macroprocesso,1.2.1 Nome do responsável pelo Macroprocesso,1.2.2 Nome do Responsável pelo Processo de Negócio,1.2.3 Nome do responsável pelo preenchimento,1.2.4  Informações de contato do responsável pelo preenchimento (RE\\, e-mail e celular),1.2.5 Nome do suplente do responsável pelo preenchimento,1.2.6 Informações de contato do suplente do responsável pelo preenchimento (RE\\, e-mail e celular),1.3.1 Razão Social,1.3.2  País em que o Controlador está localizado,1.3.3 Informações de contato do Controlador,1.4.1 Razão Social,1.4.2 País em que o Operador está localizado,1.4.3 Informações de contato do operador,,,,,,,,,,,,4.1.1 Quais dados são utilizados neste processo de negócio? (Vide guide),4.1.2 Estes dados são classificado como:\\n1) dado pessoal; ou \\n2) dado pessoal sensível?,4.1.3 Qual o perfil do titular dos dados? \\n(Vide guide),4.1.4 Descreva o propósito do tratamento de cada dado coletado,4.1.5 Qual a Base Legal utilizada para o tratamento do dado pessoal?,4.1.6 Este dado é utilizado para atividades de profiling para atingir a finalidade do processo de negócio?,4.1.7 Quais sistemas/aplicações são utilizados no tratamento destes dados pessoais?,5.1.1 Se o dado for coletado diretamente do titular dos dados\\, indique o canal,,,,,,,,5.1.2 Se o dado for coletado diretamente de um sistema interno ou processo de negócio\\, indique o canal,,,,,,,5.1.3 Se os dado for coletado diretamente de terceiros\\, indique o canal utilizado (Vide Guide),,,,,,,,,,,5.1.4  Os dados pessoais coletados são estritamente necessários para que o processo de negócio atinja o seu objetivo?  (Vide Guide),5.2.1 Os dados pessoais são tratados ou compartilhados com Terceiros DENTRO do Território Brasileiro? (Vide Guide),,,,,,,5.2.2 Os dados pessoais são tratados ou compartilhados com terceiros FORA do Território Brasileiro\\, localizados em países reconhecidos pela ANPD Autoridade Nacional de Proteção de Dados como adequados? (nível de segurança equivalente) (Vide Guide),,,,,,,5.2.3 Dados pessoais tratados ou compartilhados com terceiros FORA do Território Brasileiro\\, localizados em países NÃO reconhecidos pela ANPD Autoridade Nacional de Proteção de Dados como adequados (nível de segurança equivalente),,,,,,,6.1.1 Sim/Não,6.1.2 Qual base legal é utilizada para justificar o armazenamento do dado?,6.1.3 Qual é o período de retenção dos dados armazenados?,6.1.4 Informar o local de armazenamento Lógico (se aplicável),6.1.5 Informar o local de armazenamento Físico (se aplicável),6.1.6 Os dados pessoais são armazenados por terceiros?,6.1.7 Em caso positivo\\, informar a razão social e país onde o terceiro está localizado,6.2.1 Houve a confirmação da equipe técnica de que os dados pessoais foram excluídos?,6.2.2 Em caso positivo\\, informar data e responsável pela confirmação,7.1.2 O processo de negócio/Produto/Serviço possui formulário de Privacy by Design? S/N,7.1.3 Em caso positivo\\, informar ID do relatório,7.2.1 O processo de negócio/Produto/Serviço já foi alvo de questionamento da Autoridade de Nacional Proteção de Dados?  S/N,7.2.2 Em caso positivo\\, fornecer data e detalhes do questionamento,7.2.3 Em caso positivo\\, fornecer medidas adotas pela área para cooperação com a autoridade,7.2.4 O processo de negócio/Produto/Serviço já foi alvo de sanções?  S/N,7.2.5 Em caso positivo\\, fornecer data e detalhes da vulnerabilidade que ocasionou a sanção,7.2.6 Em caso positivo\\, fornecer ações adotadas para mitigar a vulnerabilidade que ocasionou a sanção,7.3.1 Houve algum incidente de segurança relacionado ao Processo de Negócio/Produto/Serviço em questão? S/N,7.3.2 Em caso positivo\\, fornecer data e detalhes do incidente,7.3.3 Em caso positivo\\, o relatório de incidente foi preenchido? S/N,7.3.4 Em caso positivo\\, fornecer ID do relatório,7.4.1 \"Consentimento\" foi utilizado como base legal para o tratamento de dados pessoais? S/N\n" +
      ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,5.1.1.1 Sim/Não,5.1.1.2 Website,5.1.1.3 Telefone / Call Center,5.1.1.4 E-mail,5.1.1.5 Correspondência,5.1.1.6 Aplicativos voltados ao cliente?,5.1.1.7 Liste os nomes dos aplicativos voltados ao cliente,5.1.1.8 Pessoalmente (exemplo: assinatura de contratos em lojas físicas),5.1.2.1 Sim/Não,5.1.2.2 Telefone,5.1.2.3 E-mail,5.1.2.4 Correspondência,5.1.2.5 Listar os processos de negócios internos,5.1.2.6 Sistemas/ aplicações internas?,5.1.2.7 Listar os sistemas/ aplicações utilizados para coletar os dados pessoais,5.1.3.1 Sim/Não,5.1.3.2 Website,5.1.3.3 Telefone/ Call Center,5.1.3.4 E-mail,5.1.3.5 Correspondências,5.1.3.6 Listar os terceiros,5.1.3.7 Sistema/ aplicações dos terceiros (Consultar Guia),5.1.3.8 Listar os sistemas/ aplicações utilizados pelos terceiros para a coleta de dados pessoais,5.1.3.9 Os terceiros foram submetidos ao processo de Due Diligence referentes a aspectos de proteção e privacidade de dados? S/N,5.1.3.10 Os terceiros foram satisfatoriamente aprovados na Due Diligence proteção e privacidade de dados? S/N,5.1.3.11 Indicar ID do relatório e data de avaliação do terceiro,,5.2.1.1 Sim/Não,5.2.1.2 Informar a Razão Social,5.2.1.3 Descrever o propósito da transferência,5.2.1.4 Descrever os métodos de transferência,5.2.1.5 Os terceiros foram submetidos ao processo de Due Diligence referentes a aspectos de proteção e privacidade de dados? S/N,5.2.1.6 Os terceiros foram satisfatoriamente aprovados na Due Diligence proteção e privacidade de dados? S/N,5.2.1.7 Indicar ID do relatório de Due Diligence e data de avaliação do terceiro,5.2.2.1 Sim/Não,5.2.2.2 Informar a Razão Social/País,5.2.2.3 Descrever o propósito da transferência,5.2.2.4 Descrever os métodos de transferência,5.2.2.5 Os terceiros foram submetidos ao processo de Due Diligence referentes a aspectos de proteção e privacidade de dados? S/N,5.2.2.6 Os terceiros foram satisfatoriamente aprovados na Due Diligence proteção e privacidade de dados? S/N,5.2.2.7 Indicar ID do relatório de Due Diligence e data de avaliação do terceiro,5.2.3.1 Sim/Não,5.2.3.2 Informar a Razão Social/País,5.2.3.3 Descrever o propósito da transferência,5.2.3.4 Descrever os métodos de transferência,5.2.3.5 Os terceiros foram submetidos ao processo de Due Diligence referentes a aspectos de proteção e privacidade de dados? S/N,5.2.3.6 Os terceiros foram satisfatoriamente aprovados na Due Diligence proteção e privacidade de dados? S/N,5.2.3.7 Indicar ID do relatório de Due Diligence e data de avaliação do terceiro,,,,,,,,,,,,,,,,,,,,,\n" +
      "Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Mandatório\n";

  /**
   * Test of onTrigger method RoPA headers
   */

  public static void testRoPAHeadersHelper(String mergeStrategy, String expectedRes) {
    // Content to be mock a json file
    InputStream header = new ByteArrayInputStream(ropaHeader.getBytes());

    // Generate a test runner to mock a processor in a flow
    CleanCSVHeader throttle = new CleanCSVHeader();
    TestRunner runner = TestRunners.newTestRunner(throttle);


    // Add properties

    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_MERGE_STRATEGY, mergeStrategy);
    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_COUNT, "3");
    runner.setProperty(CleanCSVHeader.USE_REGEX, "true");
    runner.setProperty(CleanCSVHeader.REMOVE_ACCENTS, "true");
    runner.setProperty(CleanCSVHeader.CSV_FIND_TEXT, "(?!(\\d+\\.{0,1})+)[ |A-Z|a-z].*");
    runner.setProperty(CleanCSVHeader.CSV_REPLACE_TEXT, "");


    // Add the content to the runner
    runner.enqueue(header);

    runner.run(1);

    List<MockFlowFile> headerResults = runner.getFlowFilesForRelationship(CleanCSVHeader.SUCCESS);


    assertEquals("1 flow file", 1, headerResults.size());
    headerResults.get(0).assertContentEquals(expectedRes);


    headerResults.clear();
    runner.enqueue(header);


  }

  @org.junit.Test
  public void testRoPAHeadersReplaceStrategy()  {
     testRoPAHeadersHelper("REPLACE",
        "pg_1.1.1,pg_1.1.2,pg_1.1.3,pg_1.1.4,pg_1.1.5,pg_1.2.1,pg_1.2.2,pg_1.2.3,pg_1.2.4,pg_1.2.5,pg_1.2.6,pg_1.3.1,pg_1.3.2,pg_1.3.3,pg_1.4.1,pg_1.4.2,pg_1.4.3,pg_1.5,pg_2.1,pg_2.2,pg_2.3,pg_2.4,pg_2.5,pg_2.6,pg_2.7,pg_3.1,pg_3.2,pg_3.3,pg_4.1.1,pg_4.1.2,pg_4.1.3,pg_4.1.4,pg_4.1.5,pg_4.1.6,pg_4.1.7,pg_5.1.1.1,pg_5.1.1.2,pg_5.1.1.3,pg_5.1.1.4,pg_5.1.1.5,pg_5.1.1.6,pg_5.1.1.7,pg_5.1.1.8,pg_5.1.2.1,pg_5.1.2.2,pg_5.1.2.3,pg_5.1.2.4,pg_5.1.2.5,pg_5.1.2.6,pg_5.1.2.7,pg_5.1.3.1,pg_5.1.3.2,pg_5.1.3.3,pg_5.1.3.4,pg_5.1.3.5,pg_5.1.3.6,pg_5.1.3.7,pg_5.1.3.8,pg_5.1.3.9,pg_5.1.3.10,pg_5.1.3.11,pg_5.1.4,pg_5.2.1.1,pg_5.2.1.2,pg_5.2.1.3,pg_5.2.1.4,pg_5.2.1.5,pg_5.2.1.6,pg_5.2.1.7,pg_5.2.2.1,pg_5.2.2.2,pg_5.2.2.3,pg_5.2.2.4,pg_5.2.2.5,pg_5.2.2.6,pg_5.2.2.7,pg_5.2.3.1,pg_5.2.3.2,pg_5.2.3.3,pg_5.2.3.4,pg_5.2.3.5,pg_5.2.3.6,pg_5.2.3.7,pg_6.1.1,pg_6.1.2,pg_6.1.3,pg_6.1.4,pg_6.1.5,pg_6.1.6,pg_6.1.7,pg_6.2.1,pg_6.2.2,pg_7.1.2,pg_7.1.3,pg_7.2.1,pg_7.2.2,pg_7.2.3,pg_7.2.4,pg_7.2.5,pg_7.2.6,pg_7.3.1,pg_7.3.2,pg_7.3.3,pg_7.3.4,pg_7.4.1\n" +
            "Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Mandatório\n");

  }

  @org.junit.Test
  public void testRoPAHeadersColNumStrategy()  {
     testRoPAHeadersHelper("COL_NUM",
        "pg_1,pg_2,pg_3,pg_4,pg_5,pg_6,pg_7,pg_8,pg_9,pg_10,pg_11,pg_12,pg_13,pg_14,pg_15,pg_16,pg_17,pg_18,pg_19,pg_20,pg_21,pg_22,pg_23,pg_24,pg_25,pg_26,pg_27,pg_28,pg_29,pg_30,pg_31,pg_32,pg_33,pg_34,pg_35,pg_36,pg_37,pg_38,pg_39,pg_40,pg_41,pg_42,pg_43,pg_44,pg_45,pg_46,pg_47,pg_48,pg_49,pg_50,pg_51,pg_52,pg_53,pg_54,pg_55,pg_56,pg_57,pg_58,pg_59,pg_60,pg_61,pg_62,pg_63,pg_64,pg_65,pg_66,pg_67,pg_68,pg_69,pg_70,pg_71,pg_72,pg_73,pg_74,pg_75,pg_76,pg_77,pg_78,pg_79,pg_80,pg_81,pg_82,pg_83,pg_84,pg_85,pg_86,pg_87,pg_88,pg_89,pg_90,pg_91,pg_92,pg_93,pg_94,pg_95,pg_96,pg_97,pg_98,pg_99,pg_100,pg_101,pg_102,pg_103,pg_104,pg_105\n" +
            "Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Mandatório,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Mandatório,Condicional,Condicional,Condicional,Mandatório\n");

  }

}
