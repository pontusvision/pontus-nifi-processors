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
public class TestCleanCSVAccentHeader {
  final static String accentHeader = "Identificação do Processo,Responsável pelo Processo de Negócio.\n" +
      "Mandatório,Mandatório\n";

  final static String sampleData1 = "NOME,EMAIL,TELEFONE,RENDA,IMÓVEL INTERESSE,END. IMÓVEL,CONSENTIMENTO,CORRETOR\n" +
      "João da Silva,joao.silva@gmail.com,11 91234-5678,\"R$ 15,000.00\",Stand A,Morumbi - SP,X,Ferrari\n" +
      "Maria de Souza,mariasouza@hotmail.com,11 98888-1234,\"R$ 8,000.00\",Stand B,Itaim Bibi - SP,,Mustang\n" +
      "José Santos,josessantos_123@gmail.com,19 92222-9999,\"R$ 5,000.00\",Stand C,Campinas - SP,X,Hebe\n" +
      "Ana Ferreira,ferreira.ana@icloud.com,21 97878-0000,\"R$ 10,000.00\",Stand D,Barra Tijuca - RJ,X,Petra\n" +
      "Eduardo Antunes,eduardoantunes_77@gmail.com,15 92345-8765,\"R$ 5,000.00\",Stand F,Sorocaba - SP,,Madri\n" +
      "Francisco Nascimento,francisco_nascimento_4422@hotmail.com,11 98777-6576,\"R$ 10,000.00\",Stand A,Morumbi - SP,X,Rubi\n" +
      "Roberto Silva,silva.robertosilva@outlook.com,11 93456-0000,\"R$ 12,000.00\",Stand G,Brooklin - SP,,Kovac\n" +
      "Cássia Dias,cassiadias_@gmail.com,51 92323-4545,\"R$ 5,000.00\",Stand J,Guaiba - RS,,Máximo\n" +
      "Bruna Passos,passos.bruna_22@hotmail.com,11 93287-7867,\"R$ 5,000.00\",Stand A,Morumbi - SP,X,Esmeralda\n" +
      "Renata Costa,renatacosta_costa@gmail.com,11 95656-9898,\"R$ 12,000.00\",Stand G,Brooklin - SP,X,Ferrari\n" +
      "Carlos Pereira,carlospereira_9900@hotmail.com,13 90089-3434,\"R$ 7,000.00\",Stand I,Santos - SP,X,Mikonos\n" +
      "Gabriel Oliveira,gabrieloliveira1234@uol.com.br,21 96545-2323,\"R$ 15,000.00\",Stand D,Barra Tijuca - RJ,X,Petra\n" +
      "Daniele Rodrigues,rodriguesdaniel_0088@gmail.com,11 98343-4242,\"R$ 20,000.00\",Stand B,Itaim Bibi - SP,X,Mustang\n" +
      "Fabio Almeida,almeidafabio.almeida@uol.com.br,51 90897-2487,\"R$ 8,000.00\",Stand J,Guaiba - RS,,Máximo\n" +
      "Otavio Gomes,otaviogomes.otavio@hotmail.com,19 92345-6234,\"R$ 20,000.00\",Stand C,Campinas - SP,,Hebe\n" +
      "Janaina Rocha,janaa_rochaa@gmail.com,13 98080-6565,\"R$ 8,000.00\",Stand I,Santos - SP,X,Mikonos\n" +
      "José Alves,alves_jose_19651030@hotmail.com,21 92121-4343,\"R$ 15,000.00\",Stand E,Copacabana - RJ,,Petra\n" +
      "Pedro Barbosa,pedrobarbosa.filho@icloud.com,11 90810-8787,\"R$ 7,000.00\",Stand H,Vila Mariana - SP,X,Kovac\n" +
      "Camila Barbosa,camilabarbosa_camila@gmail.com,11 98273-7108,\"R$ 12,000.00\",Stand H,Vila Mariana - SP,X,Esmeralda\n";

  final static String sampleData1ExpRes = "pg_NOME,pg_EMAIL,pg_TELEFONE,pg_RENDA,pg_IMOVELINTERESSE,pg_ENDIMOVEL,pg_CONSENTIMENTO,pg_CORRETOR\n" +
      "João da Silva,joao.silva@gmail.com,11 91234-5678,\"R$ 15,000.00\",Stand A,Morumbi - SP,X,Ferrari\n" +
      "Maria de Souza,mariasouza@hotmail.com,11 98888-1234,\"R$ 8,000.00\",Stand B,Itaim Bibi - SP,,Mustang\n" +
      "José Santos,josessantos_123@gmail.com,19 92222-9999,\"R$ 5,000.00\",Stand C,Campinas - SP,X,Hebe\n" +
      "Ana Ferreira,ferreira.ana@icloud.com,21 97878-0000,\"R$ 10,000.00\",Stand D,Barra Tijuca - RJ,X,Petra\n" +
      "Eduardo Antunes,eduardoantunes_77@gmail.com,15 92345-8765,\"R$ 5,000.00\",Stand F,Sorocaba - SP,,Madri\n" +
      "Francisco Nascimento,francisco_nascimento_4422@hotmail.com,11 98777-6576,\"R$ 10,000.00\",Stand A,Morumbi - SP,X,Rubi\n" +
      "Roberto Silva,silva.robertosilva@outlook.com,11 93456-0000,\"R$ 12,000.00\",Stand G,Brooklin - SP,,Kovac\n" +
      "Cássia Dias,cassiadias_@gmail.com,51 92323-4545,\"R$ 5,000.00\",Stand J,Guaiba - RS,,Máximo\n" +
      "Bruna Passos,passos.bruna_22@hotmail.com,11 93287-7867,\"R$ 5,000.00\",Stand A,Morumbi - SP,X,Esmeralda\n" +
      "Renata Costa,renatacosta_costa@gmail.com,11 95656-9898,\"R$ 12,000.00\",Stand G,Brooklin - SP,X,Ferrari\n" +
      "Carlos Pereira,carlospereira_9900@hotmail.com,13 90089-3434,\"R$ 7,000.00\",Stand I,Santos - SP,X,Mikonos\n" +
      "Gabriel Oliveira,gabrieloliveira1234@uol.com.br,21 96545-2323,\"R$ 15,000.00\",Stand D,Barra Tijuca - RJ,X,Petra\n" +
      "Daniele Rodrigues,rodriguesdaniel_0088@gmail.com,11 98343-4242,\"R$ 20,000.00\",Stand B,Itaim Bibi - SP,X,Mustang\n" +
      "Fabio Almeida,almeidafabio.almeida@uol.com.br,51 90897-2487,\"R$ 8,000.00\",Stand J,Guaiba - RS,,Máximo\n" +
      "Otavio Gomes,otaviogomes.otavio@hotmail.com,19 92345-6234,\"R$ 20,000.00\",Stand C,Campinas - SP,,Hebe\n" +
      "Janaina Rocha,janaa_rochaa@gmail.com,13 98080-6565,\"R$ 8,000.00\",Stand I,Santos - SP,X,Mikonos\n" +
      "José Alves,alves_jose_19651030@hotmail.com,21 92121-4343,\"R$ 15,000.00\",Stand E,Copacabana - RJ,,Petra\n" +
      "Pedro Barbosa,pedrobarbosa.filho@icloud.com,11 90810-8787,\"R$ 7,000.00\",Stand H,Vila Mariana - SP,X,Kovac\n" +
      "Camila Barbosa,camilabarbosa_camila@gmail.com,11 98273-7108,\"R$ 12,000.00\",Stand H,Vila Mariana - SP,X,Esmeralda\n";


  /**
   * Test of onTrigger method RoPA headers
   */

  public static void testAccentHeadersHelper(String data, String mergeStrategy, String expectedRes) {
    // Content to be mock a json file
    InputStream header = new ByteArrayInputStream(data.getBytes());

    // Generate a test runner to mock a processor in a flow
    CleanCSVHeader throttle = new CleanCSVHeader();
    TestRunner runner = TestRunners.newTestRunner(throttle);


    // Add properties

    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_MERGE_STRATEGY, mergeStrategy);
    runner.setProperty(CleanCSVHeader.MULTI_LINE_HEADER_COUNT, "1");
    runner.setProperty(CleanCSVHeader.USE_REGEX, "true");
    runner.setProperty(CleanCSVHeader.REMOVE_ACCENTS, "true");
    runner.setProperty(CleanCSVHeader.CSV_FIND_TEXT, "[\\.| ]");
    runner.setProperty(CleanCSVHeader.CSV_REPLACE_TEXT, "");
    runner.setProperty(CleanCSVHeader.CSV_PROCESSOR_FORMAT, "CUSTOM");

    runner.setValidateExpressionUsage(true);
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
  public void testAccentHeaderReplaceStrategy()  {
    testAccentHeadersHelper(accentHeader,"REPLACE",
        "pg_IdentificacaodoProcesso,pg_ResponsavelpeloProcessodeNegocio\n" +
            "Mandatório,Mandatório\n");

  }

  @org.junit.Test
  public void testAccentHeaderColNumStrategy()  {
    testAccentHeadersHelper(accentHeader,"COL_NUM",
        "pg_1,pg_2\n" +
            "Mandatório,Mandatório\n");

  }

  @org.junit.Test
  public void testSampleDataReplaceStrategy()  {
    testAccentHeadersHelper(sampleData1,"REPLACE",sampleData1ExpRes);

  }


}
